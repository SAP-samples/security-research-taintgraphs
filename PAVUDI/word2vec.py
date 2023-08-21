import glob
import os
import re

import numpy as np
import networkx as nx
from gensim.models import KeyedVectors
from gensim.models.callbacks import CallbackAny2Vec
from gensim.models.word2vec import Word2Vec
from tqdm import tqdm


class Word2VecBuilder(object):
    def __init__(self, params, overwrite_cache=False):
        self.params = params
        self.vector_size = params["vector_size"]
        self.window = params["window"]
        self.word_vectors = None
        self.overwrite_cache = overwrite_cache
        self.cache_dir = params["cache_dir"]

        if not os.path.exists(self.cache_dir):
            os.mkdir(self.cache_dir)
    
    def get_corpus(self):
        corpus = []

        for directory, _modern in self.params["training_files"]:
            c_files = list(glob.glob(os.path.join(directory, "**", "*.c")))
            if len(c_files) > 0:
                print(f"Loading C files for {directory}")
                for path in tqdm(c_files):
                    with open(path, "r", encoding='utf-8', errors='ignore') as f:
                        tokenizer = ParseTokenizer()
                        corpus.append(tokenizer(f.read()))
            else:
                cpg_files = list(glob.glob(os.path.join(directory, "**", "*.cpg")))
                assert len(cpg_files) > 0, "Must find either cpg files or c files for code"

                print(f"Loading cpg files for {directory}")
                for path in tqdm(cpg_files):
                    codelines = list()
                    with open(path, "r", encoding='utf-8', errors='ignore') as f:
                        graph = nx.Graph(nx.drawing.nx_pydot.read_dot(f))

                        for node_id in graph:
                            node = graph.nodes[node_id]
                            if node.get("enclosing") is None:
                                print("No enclosing found for ", node, graph, path)
                                continue
                            code = node["enclosing"]
                            if code[0] == "\"" and code[-1] == "\"":
                                code = code[1:-1]
                            if node.get("label") == "TranslationUnitDeclaration":
                                codelines = code
                                break
                            codelines.append(code)


                    tokenizer = ParseTokenizer()
                    corpus.append(tokenizer("\n".join(codelines)))

        return corpus

    def build_model(self):
        word2vec_path = os.path.join(self.cache_dir, "w2v.model")
        word_vector_path = os.path.join(self.cache_dir, "w2v.wordvectors")
        if os.path.isfile(word_vector_path) and not self.overwrite_cache:
            print("Loading wordvectors.")
            self.word_vectors = KeyedVectors.load(word_vector_path, mmap="r")
        elif os.path.isfile(word2vec_path) and not self.overwrite_cache:
            print("Loading w2vmodel.")
            w2vmodel = Word2Vec.load(word2vec_path)
            word_vectors = w2vmodel.wv
            word_vectors.save(word_vector_path)
            self.word_vectors = KeyedVectors.load(word_vector_path, mmap="r")
        else:
            corpus = self.get_corpus()
            w2vmodel = Word2Vec(window=self.window, min_count=5, alpha=0.01, min_alpha=0.0000001, sample=1e-5,
                                workers=8, sg=1, hs=0, negative=5, vector_size=self.vector_size)
            w2vmodel.build_vocab(corpus)
            w2vmodel.train(corpus, total_examples=len(corpus), epochs=400,
                           compute_loss=True, callbacks=[CbPrintLoss()])
            print("Saving w2vmodel.")
            w2vmodel.save(word2vec_path)
            word_vectors = w2vmodel.wv
            word_vectors.save(word_vector_path)
            self.word_vectors = KeyedVectors.load(word_vector_path, mmap="r")

    def needs_corpus(self):
        # word2vec_path = os.path.join(self.cache_dir, "w2v.model")
        # word_vector_path = os.path.join(self.cache_dir, "w2v.wordvectors")
        # return self.overwrite_cache or (not os.path.isfile(word2vec_path) and not os.path.isfile(word_vector_path))
        return False

    def get_embedding(self, code):
        if len(code.strip()) == 0:
            return np.zeros((self.vector_size,))
        tokenizer = ParseTokenizer()
        tokenized = tokenizer(code)
        if len(tokenized) == 0:
            return np.zeros((self.vector_size,))
        if self.word_vectors is None:
            self.build_model()
        return np.mean(np.array(
            [self.word_vectors[x] if len(x) > 0 and x in self.word_vectors else np.zeros((self.vector_size,)) for x in
             tokenized]),
            axis=0
        )


# source: https://github.com/johnb110/VDPython/blob/master/clean_gadget.py
# keywords up to C11 and C++17; immutable set
keywords = frozenset({'__asm', '__builtin', '__cdecl', '__declspec', '__except', '__export', '__far16', '__far32',
                      '__fastcall', '__finally', '__import', '__inline', '__int16', '__int32', '__int64', '__int8',
                      '__leave', '__optlink', '__packed', '__pascal', '__stdcall', '__system', '__thread', '__try',
                      '__unaligned', '_asm', '_Builtin', '_Cdecl', '_declspec', '_except', '_Export', '_Far16',
                      '_Far32', '_Fastcall', '_finally', '_Import', '_inline', '_int16', '_int32', '_int64',
                      '_int8', '_leave', '_Optlink', '_Packed', '_Pascal', '_stdcall', '_System', '_try', 'alignas',
                      'alignof', 'and', 'and_eq', 'asm', 'auto', 'bitand', 'bitor', 'bool', 'break', 'case',
                      'catch', 'char', 'char16_t', 'char32_t', 'class', 'compl', 'const', 'const_cast', 'constexpr',
                      'continue', 'decltype', 'default', 'delete', 'do', 'double', 'dynamic_cast', 'else', 'enum',
                      'explicit', 'export', 'extern', 'false', 'final', 'float', 'for', 'friend', 'goto', 'if',
                      'inline', 'int', 'long', 'mutable', 'namespace', 'new', 'noexcept', 'not', 'not_eq', 'nullptr',
                      'operator', 'or', 'or_eq', 'override', 'private', 'protected', 'public', 'register',
                      'reinterpret_cast', 'return', 'short', 'signed', 'sizeof', 'static', 'static_assert',
                      'static_cast', 'struct', 'switch', 'template', 'this', 'thread_local', 'throw', 'true', 'try',
                      'typedef', 'typeid', 'typename', 'union', 'unsigned', 'using', 'virtual', 'void', 'volatile',
                      'wchar_t', 'while', 'xor', 'xor_eq', 'NULL'})
# holds known non-user-defined functions; immutable set
main_set = frozenset({'main'})
# arguments in main function; immutable set
main_args = frozenset({'argc', 'argv'})

# source: https://github.wdf.sap.corp/I534627/Reveal/blob/2daa8532a416ea414cd115670d93b1fdbfb6be42/Reveal/utils/functions/parse.py
operators3 = {'<<=', '>>='}
operators2 = {
    '->', '++', '--', '**',
    '!~', '<<', '>>', '<=', '>=',
    '==', '!=', '&&', '||', '+=',
    '-=', '*=', '/=', '%=', '&=', '^=', '|='
}
operators1 = {
    '(', ')', '[', ']', '.',
    '+', '&',
    '%', '<', '>', '^', '|',
    '=', ',', '?', ':',
    '{', '}', '!', '~'
}


def to_regex(lst):
    return r'|'.join([f"({re.escape(el)})" for el in lst])


regex_split_operators = to_regex(operators3) + to_regex(operators2) + to_regex(operators1)


# source: https://github.wdf.sap.corp/I534627/Reveal/blob/master/Reveal/utils/functions/parse.py
class ParseTokenizer(object):

    def __init__(self):
        self.fun_symbols = {}  # Dictionary; map function name to symbol name + number
        self.var_symbols = {}  # Dictionary; map variable name to symbol name + number
        self.fun_count = 1
        self.var_count = 1

    def __call__(self, code):
        if len(code) == 0:
            return []
        
        if code[0] == code[-1] and (code[0] == "'" or code[0] == '"'):
            code = code[1:-1]

        gadget = []
        tokenized = []

        # Remove all string literals
        no_str_lit_line = re.sub(r'["]([^"\\\n]|\\.|\\\n)*["]', '', code)
        # Remove all character literals
        no_char_lit_line = re.sub(r"'.*?'", "", no_str_lit_line)

        code = no_char_lit_line

        for line in code.splitlines():
            if line == '':
                continue
            stripped = line.strip()
            gadget.append(stripped)

        clean = self.clean_gadget(gadget)

        for cg in clean:
            if cg == '':
                continue

            # Remove code comments
            pat = re.compile(r'(/\*([^*]|(\*+[^*\/]))*\*+\/)|(\/\/.*)')
            cg = re.sub(pat, '', cg)

            # Remove newlines & tabs
            cg = re.sub('(\n)|(\\\\n)|(\\\\)|(\\t)|(\\r)', '', cg)

            # Mix split (characters and words)
            splitter = r' +|' + regex_split_operators + r'|(\/)|(\;)|(\-)|(\*)'
            cg = re.split(splitter, cg)

            # Remove None type
            cg = list(filter(None, cg))
            cg = list(filter(str.strip, cg))

            tokenized.extend(cg)

        return tokenized

    def clean_gadget(self, gadget):

        # Final cleaned gadget output to return to interface
        cleaned_gadget = []

        # Regular expression to find function name candidates
        rx_fun = re.compile(r'\b([_A-Za-z]\w*)\b(?=\s*\()')
        # Regular expression to find variable name candidates
        rx_var = re.compile(r'\b([_A-Za-z]\w*)\b((?!\s*\**\w+))(?!\s*\()')

        for line in gadget:

            # Replace any non-ASCII characters with empty string
            ascii_line = re.sub(r'[^\x00-\x7f]', r'', line)
            # Remove all hexadecimal literals
            hex_line = re.sub(r'0[xX][0-9a-fA-F]+', "HEX", ascii_line)
            # Return, in order, all regex matches at string list; preserves order for semantics
            user_fun = rx_fun.findall(hex_line)
            user_var = rx_var.findall(hex_line)

            for fun_name in user_fun:
                if len({fun_name}.difference(main_set)) != 0 and len({fun_name}.difference(keywords)) != 0:
                    # Check to see if function name already in dictionary
                    if fun_name not in self.fun_symbols.keys():
                        self.fun_symbols[fun_name] = 'FUN' + str(self.fun_count)
                        self.fun_count += 1
                    # ensure that only function name gets replaced (no variable name with same identifier); uses positive lookforward
                    hex_line = re.sub(r'\b(' + fun_name + r')\b(?=\s*\()', self.fun_symbols[fun_name], hex_line)

            for var_name in user_var:
                # Next line is the nuanced difference between fun_name and var_name
                if len({var_name[0]}.difference(keywords)) != 0 and len({var_name[0]}.difference(main_args)) != 0:
                    # check to see if variable name already in dictionary
                    if var_name[0] not in self.var_symbols.keys():
                        self.var_symbols[var_name[0]] = 'VAR' + str(self.var_count)
                        self.var_count += 1
                    # ensure that only variable name gets replaced (no function name with same identifier); uses negative lookforward
                    hex_line = re.sub(r'\b(' + var_name[0] + r')\b(?:(?=\s*\w+\()|(?!\s*\w+))(?!\s*\()',
                                      self.var_symbols[var_name[0]], hex_line)

            cleaned_gadget.append(hex_line)

        # Return the list of cleaned lines
        return cleaned_gadget

    # source: https://github.wdf.sap.corp/I534627/Reveal/blob/master/Reveal/utils/w2v.py


class CbPrintLoss(CallbackAny2Vec):
    '''Callback to print loss after each epoch.'''

    def __init__(self):
        self.epoch = 0
        self.prev_loss = 0

    def on_epoch_end(self, model):
        curr_loss = model.get_latest_training_loss()
        # gensim stores only the total loss (for whichever reason),
        # so we have to track the previous loss value as well
        print('Loss after epoch {}: {}'.format(self.epoch, curr_loss - self.prev_loss))
        self.epoch += 1
        self.prev_loss = curr_loss
