# Supaprastinta blokų grandinė

Vilniaus universiteto „Blokų grandinių technologijų“ 2-asis laboratorinis darbas.

## Aprašymas

Java 21 kalba sukurta supaprastinta blokų grandinės simuliacija, naudojanti pirmojo laboratorinio darbo maišos funkciją [HashFunctionV012](https://github.com/kajusviliusis/blockchain-technologies-1).

Programa generuoja vartotojus ir transakcijas, formuoja blokus, atlieka Proof-of-Work (PoW) kasimą, tikrina blokus ir atnaujina vartotojų balansus.

## v0.1 funkcionalumas

- Generuojama 1 000 vartotojų ir 10 000 transakcijų (`seed = 42`).
- Viename bloke talpinama iki 100 transakcijų.
- Transakcijos ID apskaičiuojamas iš `sender|receiver|amount|sequence`.
- `txRoot` gaunamas maišuojant sujungtus transakcijų ID.
- PoW sudėtingumas – 3 pradiniai hex nuliai (`000`).
- Naudojamas sąskaitų balansų modelis be komisinių ir kasimo atlygių.
- Genesis blokui netaikomas PoW.
- Tikrinamos transakcijos, blokų vientisumas ir balansai.

## v0.2 funkcionalumas

- `txRoot` skaičiuojamas naudojant Merkle medį: poruojami transakcijų ID hash baitai, o neporinis paskutinis mazgas dubliuojamas.
- Transakcijos tikrinamos prieš įtraukiant į fondą, atrenkant į bloką ir prieš galutinį bloko priėmimą.
- Dėl kiekvieno naujo bloko konkuruoja 5 kandidatai, naudojantys tą patį `prevHash` ir pradinę balansų būseną.
- Kandidatai kasami nuosekliai. Pirmame raunde kiekvienam skiriama iki 5 000 PoW bandymų.
- Jei nė vienas neranda PoW, bandymų biudžetas didinamas, o paieška tęsiama nuo dar netikrinto `nonce` (iki 1 000 000 bandymų kandidatui).
- Laimi tinkamą PoW per mažiausiai bandymų einamajame raunde radęs kandidatas; lygiųjų atveju – mažesnio indekso kandidatas.
- Tik laimėtojo blokas dar kartą patikrinamas, įtraukiamas į grandinę ir pakeičia balansus.

Tai vietinė konkurencijos simuliacija, **ne P2P konsensusas**: nėra nepriklausomų tinklo mazgų, blokų siuntimo tinklu ar grandinės šakų derinimo.

## Paleidimas

Reikalingi Java 21 ir Maven.

Kompiliavimas:

```bash
mvn clean compile
```

Programos paleidimas:

```bash
mvn exec:java -Dexec.mainClass="lt.vu.blockchain.Main"
```

Testų paleidimas:

```bash
mvn clean test
```

## Rezultatai

### Bloko pavyzdys (v0.2)

```text
Block height: 1, transactions: 100
  Previous hash: 55a0d7374561c65c59ffdabe632e1a1436a9240442a6dbc5ece24a8330c9a54c
  Transaction root: 66e707c9d5a4570a16a171abcc3248284469a3112303b796caadab6f4f4c2d9d
  Block hash: 000e48aee1a3bef260ff71621488b49df52fee0007e503c23eaee212558936c2
  Difficulty: 3, nonce: 2934
  Round 1:
    Candidate 1: 2935 attempts (total 2935), 9 ms, PoW found
    Candidate 2: 5000 attempts (total 5000), 14 ms, no PoW
    Candidate 3: 5000 attempts (total 5000), 14 ms, no PoW
    Candidate 4: 4702 attempts (total 4702), 13 ms, PoW found
    Candidate 5: 3119 attempts (total 3119), 10 ms, PoW found
    Winner: candidate 1
  Mining attempts: 2935, duration: 9 ms
```

### Galutinė suvestinė (v0.2)

```text
Generated users: 1000
Generated transactions: 10000
Accepted non-genesis blocks: 100
Confirmed transactions: 9950
Rejected transactions: 0
Remaining pending transactions: 50
Initial total balance: 498302330
Final total balance: 498302330
Total execution time: 5078 ms
```

Iš 10 000 sugeneruotų transakcijų patvirtinta 9 950 (99,5 %). Likusios 50 neįvykdytos dėl nepakankamo siuntėjų balanso. Pradinė ir galutinė lėšų suma sutampa.

### Testai

JUnit 5 testais tikrinamas Merkle medis, transakcijų ir blokų validacija, balansai, PoW ir kandidatų konkurencija. `mvn clean test` rezultatas – **15 testų, 0 klaidų**.

## DI naudojimas

Kuriant v0.1 ir v0.2 naudoti ChatGPT ir OpenAI Codex – planavimui, kodo generavimui, peržiūrai ir testams.
