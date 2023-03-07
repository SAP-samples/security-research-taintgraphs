#include <stdio.h>
#include <string.h>

int copy(FILE *f, char *buffer) {
    int len1;
    fread((char *)&len1, sizeof(int), 1, f);

    int len2 = len1;
    if (len2 > 256) {
        return;
    }
    char local[256];
    memcpy(local, buf, len1);
}