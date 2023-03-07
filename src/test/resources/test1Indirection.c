#include <stdio.h>
#include <stdlib.h>



int main(void)
 {
         int bla[2];
         scanf("%d", &bla[0]);
         scanf("%d", &bla[1]);

        *(bla+1)=3;
         create(bla[0],bla[1]);


 }

