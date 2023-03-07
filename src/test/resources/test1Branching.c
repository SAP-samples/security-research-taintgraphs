#include <stdio.h>
#include <stdlib.h>



int main(void)
 {
         int a;
         int b;
         scanf("%d", &a);
         scanf("%d", &b);
         a = 3+a;
         create(a,b);

         for(int i = a; i<b; i++){
            create(i,i*2);
         }

 }

