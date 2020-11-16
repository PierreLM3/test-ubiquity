Partie Akka:  fichier TechnicalTest.scala
===============

1. Il s'agit de concevoir deux acteurs très simples:
- un acteur Producteur (P);
- un acteur Consommateur (C).

P génère un flux de "NewPoint" (cf. la fonction NewPoint.random() pour la génération) qu'il envoie à C.

C stocke l'instance de NewPoint dans une structure de données de type vecteur, *de taille bornée*.
C exécute Pav.regression sur cette structure de données.
Attention!: Pav.regression exige en entrée une structure de données triée selon les "x".

On pourra indifféremment utiliser akka-classic ou akka-typed. (Notre base de
code est en akka-classic, mais nous envisageons à terme de migrer vers
akka-typed.)


2. Le problème que l'on veut étudier est le suivant: que se passe-t-il si P
émet plus vite que la vitesse de traitement de C?  Plusieurs possibilités sont
envisageables (ex. gestion de la back-pressure). Ici, on se place dans le cadre
suivant: si C sature, on choisit de ne pas traiter l'excédent de messages.

- configurer C de manière à ce qu'il utilise une file de message bornée [1].

- lorsque la file de message sature, le comportement de
NonBlockingBoundedMailbox est de rediriger l'excédent vers la "deadLetter".
Or, ce comportement ne nous convient pas: on souhaite le considèrer comme
"normal", et nous contenter d'émettre une métrique pour en garder trace.

Il s'agit alors de développer une autre implémentation de
NonBlockingBoundedMailbox, qui, lorsque la taille de la queue est atteinte, ne
redirige pas vers "deadLetter", mais permet un comportement personnalisée (pour
l'exercice, on se contentera de logguer qu'il y a un dépassement, par exemple).


[1] https://doc.akka.io/api/akka/current/akka/dispatch/NonBlockingBoundedMailbox.html
