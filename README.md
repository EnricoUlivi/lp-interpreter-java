# Progetto finale LP a.a. 2025-2026

Il progetto finale consiste nell'implementazione di un'estensione del linguaggio sviluppato durante gli ultimi laboratori Java; la soluzione proposta per l'ultimo laboratorio può essere utilizzata come base di partenza. È comunque richiesto che le implementazioni della semantica statica e dinamica siano basate sul **visitor pattern**.

L'interfaccia da linea di comando per interagire con l'interprete deve prevedere:
- il programma da eseguire può essere letto da un file di testo con l’opzione `-i <filename>`, altrimenti viene letto dallo standard input.
- l'output del programma in esecuzione può essere salvato su un file di testo con l’opzione `-o <filename>`, altrimenti viene usato lo standard output.
- l’opzione `-ntc` (no-type-checking) permette di eseguire il programma senza effettuare prima il controllo di semantica statica del typechecker.

## Definizione del linguaggio

### Sintassi
Il linguaggio introduce i seguenti nuovi tipi di token:
- `CAT` (simbolo `@`)
- `ZIP` (simbolo `++`)
- `FLATTEN` (simbolo `!!`)
- `OPEN_VECT` (simbolo `[`)
- `CLOSE_VECT` (simbolo `]`)
- `FOR` (parola chiave `for`)
- `IN` (parola chiave `in`)

La sintassi del linguaggio è definita dalla seguente grammatica EBNF non ambigua:

```ebnf
Prog ::= StmtSeq EOF
StmtSeq ::= Stmt (STMT_SEP StmtSeq)?
Stmt ::= VAR? IDENT ASSIGN Exp | PRINT Exp | IF OPEN_PAR Exp CLOSE_PAR Block (ELSE Block)? | ASSERT Exp | FOR OPEN_PAR VAR IDENT IN Exp CLOSE_PAR Block
Block ::= OPEN_BLOCK StmtSeq CLOSE_BLOCK
Exp ::= And (PAIR_OP And)*
And ::= Eq (AND Eq)*
Eq ::= Zip (EQ Zip)*
Zip ::= Add (ZIP Add)*
Add ::= Mul (PLUS Mul)*
Mul ::= Cat (TIMES Cat)*
Cat ::= Atom (CAT Atom)*
Atom ::= FST Atom | SND Atom | MINUS Atom | NOT Atom | BOOL | NUM | IDENT | OPEN_PAR Exp CLOSE_PAR | FLATTEN Atom | OPEN_VECT Exp CLOSE_VECT 
```
**Principali aggiunte rispetto al linguaggio base:**

- `Stmt ::= FOR OPEN_PAR VAR IDENT IN Exp CLOSE_PAR Block`: istruzione di iterazione sugli elementi di un vettore (es. `for(var x in [1]@[2]){print x}`).
- `Zip ::= Add (ZIP Add)*`: operatore di zip `++` tra vettori (es. `[1]++[true]`).
- `Cat ::= Atom (CAT Atom)*`: concatenazione di vettori tramite l'operatore `@` (es. `[1]@[2]`).
- `Atom ::= FLATTEN Atom`: operatore `!!` di flatten ("appiattimento") di vettori di vettori (es. `!![[1]]`).
- `Atom := OPEN_VECT Exp CLOSE_VECT`: costruttore di un vettore singleton (es. `[1]`).

### Semantica statica
La semantica statica è definita in modo preciso nel file `semantics/Semantics.fs`. 

Ogni vettore deve contenere elementi di uno stesso tipo prefissato. Il tipo `vettore` contiene due informazioni: il tipo dei suoi elementi e la sua dimensione, ossia il numero di elementi che contiene.

**Regole di tipo:**

- **For-each:** l'espressione `Exp` deve avere tipo `vettore` di elementi e dimensione qualsiasi. La variabile di iterazione, dichiarata nello statement, ha lo stesso tipo degli elementi del vettore e viene dichiarata in un nuovo livello di scope, immediatamente annidato in quello del `for-each`. La semantica statica del blocco del `for-each` è quella usuale, come accade per lo statement `if`. Quindi esistono tre distinti livelli di scope: quello più esterno del `for-each`; quello intermedio, che contiene solo la variabile del `for-each`; quello più interno che corrisponde al blocco definito nello statement `for-each`, che viene gestito automaticamente dalla semantica statica del blocco.
- **Addizione:** permette di addizionare due interi, oppure due vettori di interi che devono avere la stessa dimensione. Nel caso dei vettori, il risultato è un vettore di interi della stessa dimensione degli argomenti. 
- **Moltiplicazione:** permette di moltiplicare due interi, oppure due vettori di interi che possono avere dimensioni diverse. Nel caso dei vettori, se `size1` e `size2` sono rispettivamente le dimensioni del primo e secondo argomento, allora il risultato è un vettore di dimensione `size2` i cui elementi sono vettori di interi di dimensione `size1`.
- **Operatori fst e snd:** permettono di estrarre la prima e seconda componente di una coppia o di un vettore di coppie. Nel caso del vettore, se `T1*T2` è il tipo degli elementi del vettore e `size` la sua dimensione, allora il risultato è un vettore di dimensione `size` e di elementi di tipo `T1`, nel caso di `fst`, o `T2` nel caso di `snd`.
- **Zip:** permette di combinare due vettori della stessa dimensione, che però possono avere elementi di tipi diversi. Se `elemType1` e `elemType2` sono rispettivamente i tipi degli elementi del primo e secondo argomento, allora il risultato è un vettore di elementi di tipo `elemType1*elemType2` e della stessa dimensione degli argomenti.
- **Concatenazione:** permette di combinare due vettori di elementi dello stesso tipo `elemType`, ma di dimensioni anche diverse. Se `size1` e `size2` sono rispettivamente le dimensioni del primo e secondo argomento, allora il risultato è un vettore di elementi di tipo `elemType` e di dimensione `size1+size2`.
- **Flatten:** permette di "appiattire" un vettore i cui elementi sono a loro volta dei vettori. Se l'argomento è un vettore di dimensione `size1` e i suoi elementi sono vettori di dimensione `size2` e di elementi di tipo `elemType`, allora il risultato è un vettore di elementi di tipo `elemType` e dimensione `size1*size2`.
- **Vettore singleton:** permette di definire vettori di dimensione 1. Il tipo degli elementi coincide con in tipo dell'espressione. 
    
## Semantica dinamica
La semantica dinamica è definita in modo preciso nel file `semantics/Semantics.fs`. 

La semantica delle operazioni sui vettori è funzionale, nel senso che tutte restituiscono come risultato dei nuovi vettori e non modificano gli argomenti. 

Ogni vettore è una sequenza di valori. 

Due vettori `[u_1,...,u_m]` e `[v_1,...,v_n]` sono uguali se e solo `m=n` e `u_1=v_1,...,u_m=v_m`.  

La stampa del vettore `[u_1,...,u_m]` produce la stringa `"VectorValue[m]"`.

**Regole semantiche:**

- **For-each:** viene valutata l'espressione, il cui valore deve essere un vettore di valori `[v_1,...,v_n]`. In questo caso, la variabile di iterazione, dichiarata nello statement, viene dichiarata in un nuovo livello di scope, immediatamente annidato in quello del `for-each`, e viene inizializzata con un valore qualsiasi (per esempio 0), che non ha effetto sulla semantica finale dello statement. Viene ripetuta per n volte l'esecuzione del blocco dello statement. Nel giro di iterazione i-mo, prima di eseguire il blocco, viene assegnato il valore `v_i` alla variabile di iterazione. Al termine dell'esecuzione dello statement `for-each`, viene eliminato il livello di scope della variabile di iterazione.
- **Addizione:** l'addizione dei vettori `[i_1,...,i_m]` e `[j_1,...,j_n]` è definita solo se `m=n` e tutti i valori dei due vettori sono interi. Il risultato è `[i_1+j_1,...,j_n+j_m]`. Il controllo sulle dimensioni viene eseguito prima del calcolo, quello sul tipo degli elementi viene eseguito contestualmente alle operazioni sui singoli elementi.
- **Moltiplicazione:** la moltiplicazione dei vettori `[i_1,...,i_m]` e `[j_1,...,j_n]` è definita solo se tutti i valori dei due vettori sono interi. Il risultato è il prodotto esterno dei due vettori, memorizzato per colonne, ossia `[[j_1*i_1,...,j_1*i_m],...,[j_n*i_1,...,j_n*i_m]]`.  Il controllo sul tipo degli elementi viene eseguito contestualmente alle operazioni sui singoli elementi.
- **Operatori fst e snd:** le operazioni sul vettore sono definite solo quando tutti i valori sono delle coppie, ossia l'argomento è della forma `[(u_1,v_1),...,(u_n,v_n)]`. I risultati di `fst` e `snd` sono rispettivamente, `[u_1,...,u_n]` e `[v_1,...,v_n]`. Il controllo sul tipo degli elementi viene eseguito contestualmente alle operazioni sui singoli elementi.
- **Zip:** lo zip dei vettori `[u_1,...,u_m]` e `[v_1,...,v_n]` è definita solo se `m=n`. Il risultato è `[(u_1,v_1),...,(u_m,v_m)]`. 
- **Concatenazione:** la concatenazione dei vettori `[u_1,...,u_m]` e `[v_1,...,v_n]` è sempre definita. Il risultato è `[u_1,...,u_m,v_1,...,v_n]`. 
- **Flatten:** l'operazione è definita solo se tutti gli elementi del vettore sono dei vettori, ossia l'argomento ha forma `[[v_(1,1),...,v_(1,n)],...,[v_(m,1),...,v_(m,n)]]`. Il risultato è `[v_(1,1),...,v_(1,n),...,v_(m,1),...,v_(m,n)]`. Il controllo sul tipo degli elementi viene eseguito contestualmente alle operazioni sui singoli elementi. 
- **Vettore singleton:** il risultato è il vettore `[v]`, dove `v` è il valore dell'espressione. 

## Contenuto del repository

- `semantics/`: folder con la definizione della semantica `Semantics.fs` e il programma eseguibile `Program.fs` con i test `tests/success/prog08.txt` e `failure/static-semantics/prog09.txt`.
- `tests/`: raccolta di programmi di test utilizzati per verificare le funzionalità del programma. Il risultato atteso dei test è specificato nei commenti di ogni file.

  Organizzazione dei sotto-folder:
  - `success/`: nessun errore statico o dinamico deve essere segnalato; il risultato è riportato tra commenti.
  - `failure/static-semantics`: senza opzione `-ntc`, deve essere segnalato un errore statico, con il messaggio riportato tra commenti.
  - `failure/static-semantics-ntc`: con l'opzione `-ntc`, deve essere segnalato un errore dinamico, con il messaggio riportato tra commenti.
  - `failure/static-semantics-only`: senza opzione `-ntc`, deve essere segnalato un errore statico, con il messaggio riportato tra commenti.
  - `failure/static-semantics-only-ntc`: con l'opzione `-ntc`, nessun errore statico o dinamico deve essere segnalato; il risultato è riportato tra commenti.

## Modalità di consegna

- La consegna è valida solo se il **progetto passa tutti i test** contenuti nel folder `tests`; la valutazione del progetto tiene conto dell'esecuzione di test aggiuntivi e della qualità del codice.
- Sono disponibili cinque turni di consegna con scadenze in prossimità delle date delle prove scritte.
  Il calendario sarà reso disponibile nella [sezione di AulaWeb](https://2025.aulaweb.unige.it/mod/assign/view.php?id=61316) per la consegna del progetto.

  Dopo la scadenza di ogni turno, vengono corretti i progetti consegnati. Le consegne vengono riaperte dopo la pubblicazione dei risultati relativi al turno di consegna.

  **Dopo la scadenza dell'ultimo turno del 2027 non sarà più possibile consegnare progetti validi per l'anno accademico in corso**
- Il progetto può essere consegnato anche se l'esame scritto non è stato ancora superato
- Dopo il commit (e push) finale del progetto su GitHub, la consegna va segnalata da **un singolo componente del gruppo** utilizzando [AulaWeb](https://2025.aulaweb.unige.it/mod/assign/view.php?id=61316) e indicando **il numero del gruppo** definito nell'[elenco su AulaWeb](https://2025.aulaweb.unige.it/mod/wiki/view.php?id=81491)
- Per ricevere supporto durante lo sviluppo del progetto è consigliabile tenere sempre aggiornato il codice del progetto sul repository GitHub  
- Dopo che il progetto è stato valutato positivamente, può essere sostenuta la relativa discussione **individuale**,  anche se l'esame scritto non è stato ancora superato. Il colloquio ha lo scopo di verificare che ogni componente del gruppo abbia contribuito attivamente allo sviluppo del progetto e abbia sappia ragionare sul funzionamento del codice. Tale verifica consiste nell'implementazione di una semplice estensione da sviluppare direttamente sul programma del gruppo.   
- L'**OpenBadge Soft skills - Sociale base 1 - A** verrà assegnato ai componenti del gruppo solo se **tutti** avranno superato positivamente (ossia senza decremento del punteggio) il colloquio individuale.
- Per ulteriori informazioni consultare la [pagina AulaWeb sulle modalità di esame](https://2025.aulaweb.unige.it/mod/page/view.php?id=61303)

