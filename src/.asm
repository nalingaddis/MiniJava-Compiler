  0         LOADL        0
  1         CALL         newarr  
  2         CALL         L10
  3         HALT   (0)   
  4  L10:   LOADL        -1
  5         LOADL        2
  6         CALL         newobj  
  7         LOADL        10
  8         LOAD         3[LB]
  9         CALLI        L11
 10         RETURN (0)   1
 11  L11:   LOAD         -1[LB]
 12         LOADA        0[OB]
 13         CALLI        L22
 14         LOADA        0[OB]
 15         CALLI        L12
 16         LOADA        0[OB]
 17         CALLI        L19
 18         RETURN (0)   1
 19  L12:   LOAD         1[OB]
 20         LOADL        1
 21         CALL         sub     
 22         LOADL        0
 23  L13:   LOAD         4[LB]
 24         LOAD         3[LB]
 25         CALL         lt      
 26         JUMPIF (0)   L17
 27         LOAD         0[OB]
 28         LOAD         4[LB]
 29         CALL         arrayref
 30         LOAD         0[OB]
 31         LOAD         4[LB]
 32         LOADL        1
 33         CALL         add     
 34         CALL         arrayref
 35         CALL         le      
 36         JUMPIF (0)   L14
 37         LOAD         4[LB]
 38         LOADL        1
 39         CALL         add     
 40         LOADA        4[LB]
 41         STOREI 
 42         JUMP         L16
 43  L14:   LOAD         0[OB]
 44         LOAD         4[LB]
 45         LOAD         4[LB]
 46         LOADL        1
 47         CALL         add     
 48         LOADA        0[OB]
 49         CALLI        L18
 50         LOAD         4[LB]
 51         LOADL        0
 52         CALL         gt      
 53         JUMPIF (0)   L15
 54         LOAD         4[LB]
 55         LOADL        1
 56         CALL         sub     
 57         LOADA        4[LB]
 58         STOREI 
 59         JUMP         L15
 60  L15:   POP          0
 61  L16:   POP          0
 62         JUMP         L13
 63  L17:   RETURN (0)   0
 64  L18:   LOAD         -3[LB]
 65         LOAD         -2[LB]
 66         CALL         arrayref
 67         LOAD         -3[LB]
 68         LOAD         -2[LB]
 69         LOAD         -3[LB]
 70         LOAD         -1[LB]
 71         CALL         arrayref
 72         CALL         arrayupd
 73         LOAD         -3[LB]
 74         LOAD         -1[LB]
 75         LOAD         3[LB]
 76         CALL         arrayupd
 77         RETURN (0)   3
 78  L19:   LOADL        0
 79  L20:   LOAD         3[LB]
 80         LOAD         1[OB]
 81         CALL         lt      
 82         JUMPIF (0)   L21
 83         LOAD         0[OB]
 84         LOAD         3[LB]
 85         CALL         arrayref
 86         CALL         putintnl
 87         LOAD         3[LB]
 88         LOADL        1
 89         CALL         add     
 90         LOADA        3[LB]
 91         STOREI 
 92         POP          0
 93         JUMP         L20
 94  L21:   RETURN (0)   0
 95  L22:   LOAD         -1[LB]
 96         CALL         newarr  
 97         LOADA        0[OB]
 98         STOREI 
 99         LOAD         0[OB]
100         CALL         arraylen
101         LOADA        1[OB]
102         STOREI 
103         LOADL        17
104         LOADL        1
105  L23:   LOAD         4[LB]
106         LOAD         1[OB]
107         CALL         le      
108         JUMPIF (0)   L24
109         LOAD         0[OB]
110         LOAD         4[LB]
111         LOAD         3[LB]
112         CALL         mult    
113         LOAD         1[OB]
114         LOADA        0[OB]
115         CALLI        L25
116         LOAD         4[LB]
117         CALL         arrayupd
118         LOAD         4[LB]
119         LOADL        1
120         CALL         add     
121         LOADA        4[LB]
122         STOREI 
123         POP          0
124         JUMP         L23
125  L24:   RETURN (0)   1
126  L25:   LOAD         -2[LB]
127         LOAD         -2[LB]
128         LOAD         -1[LB]
129         CALL         div     
130         LOAD         -1[LB]
131         CALL         mult    
132         CALL         sub     
133         RETURN (1)   2
