#include <stdio.h>
#include <string.h>


extern int global_variable;

int copy(FILE *f, char *buffer) {

    char local[256];
    memcpy(local, buffer, global_variable);
}