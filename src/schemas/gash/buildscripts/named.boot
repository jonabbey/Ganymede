; named.boot                 
;
; Boot file making this machine a Primary Master Name Server
; for the domain arlut.utexas.edu
;
directory					/var/named
;
; hosts files
;
primary	 	arlut.utexas.edu		named.hosts
;
; reverse mapping files 
;
primary		128.116.129.IN-ADDR.ARPA	named.128
primary		130.116.129.IN-ADDR.ARPA	named.130
primary		136.116.129.IN-ADDR.ARPA	named.136
primary		144.116.129.IN-ADDR.ARPA	named.144
primary		152.116.129.IN-ADDR.ARPA	named.152
primary		160.116.129.IN-ADDR.ARPA	named.160
primary		176.116.129.IN-ADDR.ARPA	named.176
primary		188.116.129.IN-ADDR.ARPA	named.188
primary		192.116.129.IN-ADDR.ARPA	named.192
primary		196.116.129.IN-ADDR.ARPA	named.196
primary		208.116.129.IN-ADDR.ARPA	named.208
primary		212.116.129.IN-ADDR.ARPA	named.212
primary		224.116.129.IN-ADDR.ARPA	named.224
primary		236.116.129.IN-ADDR.ARPA	named.236
primary		240.116.129.IN-ADDR.ARPA	named.240
primary		244.116.129.IN-ADDR.ARPA	named.244
primary		252.116.129.IN-ADDR.ARPA	named.252
;
; internal class C reverse map
;
primary		134.48.192.IN-ADDR.ARPA		named.134c
primary		135.48.192.IN-ADDR.ARPA		named.135c
;
; other files
;
primary		0.0.127.IN-ADDR.ARPA		named.local      
cache		.				named.cache
;
