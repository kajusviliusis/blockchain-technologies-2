# Supaprastinta blokų grandinė

Vilniaus universiteto „Blokų grandinių technologijų“ 2-asis laboratorinis darbas.

## Aprašymas

Java 21 kalba sukurta supaprastinta blokų grandinės simuliacija, naudojanti pirmojo laboratorinio darbo maišos funkciją [HashFunctionV012](https://github.com/kajusviliusis/blockchain-technologies-1).

Programa generuoja vartotojus ir transakcijas, formuoja blokus, atlieka darbo įrodymo (PoW) kasimą, tikrina blokus ir atnaujina vartotojų balansus.

## v0.1 funkcionalumas

- Generuojama 1 000 vartotojų ir 10 000 transakcijų (atsitiktinių skaičių pradinė reikšmė – 42).
- Viename bloke talpinama iki 100 transakcijų.
- Transakcijos ID apskaičiuojamas iš `sender|receiver|amount|sequence`.
- `txRoot` gaunamas maišuojant sujungtus transakcijų ID.
- PoW sudėtingumas – 3 pradiniai šešioliktainiai nuliai (`000`).
- Naudojamas sąskaitų balansų modelis be komisinių ir kasimo atlygių.
- Pradiniam (genesis) blokui netaikomas PoW.
- Tikrinamos transakcijos, blokų vientisumas ir balansai.

## v0.2 funkcionalumas

- `txRoot` skaičiuojamas naudojant Merkle medį: poruojami transakcijų ID maišų baitai, o neporinis paskutinis mazgas dubliuojamas.
- Transakcijos tikrinamos prieš įtraukiant į fondą, atrenkant į bloką ir prieš galutinį bloko priėmimą.
- Dėl kiekvieno naujo bloko konkuruoja 5 kandidatai, naudojantys tą patį `prevHash` ir pradinę balansų būseną.
- Kandidatai kasami nuosekliai. Pirmame raunde kiekvienam skiriama iki 5 000 PoW bandymų.
- Jei nė vienas neranda PoW, bandymų biudžetas didinamas, o paieška tęsiama nuo dar netikrinto `nonce` (iki 1 000 000 bandymų kandidatui).
- Laimi tinkamą PoW per mažiausiai bandymų einamajame raunde radęs kandidatas; lygiųjų atveju – mažesnio indekso kandidatas.
- Tik laimėtojo blokas dar kartą patikrinamas, įtraukiamas į grandinę ir pakeičia balansus.

Tai vietinė konkurencijos simuliacija, **ne P2P konsensusas**: nėra nepriklausomų tinklo mazgų, blokų siuntimo tinklu ar grandinės šakų derinimo.

## v0.3 – DI pagalba atlikti patobulinimai

Su **OpenAI Codex (GPT-6)** optimizuotos dvi v0.2 dalys. Originali `HashFunctionV012.java` nebuvo keičiama.

### Transakcijų atranka (`TransactionPool`)

- Vietoje transakcijų sąrašo kopijavimo maišomas pakartotinai naudojamas indeksų masyvas.
- Laikinai saugomi tik pasikeitę balansai, išvengiama nereikalingų kolekcijų kopijų.
- Transakcijų ID tikrinamas priimant į fondą, o ne pakartotinai kiekvienos atrankos metu.
- **Su vienodais duomenimis ir atsitiktinių skaičių būsena atrenkamos tos pačios transakcijos ta pačia tvarka.**

| Rodiklis (100 atrankų) | v0.2 | v0.3 | Pokytis |
| --- | ---: | ---: | ---: |
| Laikas, 1-oji JVM | 38,021 ms | 11,156 ms | **−70,7 %** |
| Laikas, 2-oji JVM | 39,205 ms | 12,557 ms | **−68,0 %** |
| Alokacijos, 1-oji JVM | 63,85 MB | 1,40 MB | **−97,8 %** |
| Alokacijos, 2-oji JVM | 58,21 MB | 1,40 MB | **−97,6 %** |

### Merkle medis (`MerkleTree`)

- Tarpiniai hash saugomi viename baitų masyve, pakartotinai naudojamas poros buferis.
- Išsaugotos identiškos Merkle šaknys ir nestandartinio ilgio ID palaikymas.
- **Alokacijų sumažėjo 3,1–3,3 %**; vykdymo laiko skirtumas (0,4–2,4 %) per mažas tvirtam pagreitėjimo teiginiui.

**DI sprendimai:** priimtos abi optimizacijos, patikrinus jas prieš v0.2 bazines realizacijas. Atmestas kasimo optimizavimo pasiūlymas, kuris apeidavo originalų maišos metodą; išsaugojus originalią funkciją, nauda buvo menka (~2,7 % viename matavime).

**Patikra:** v0.2 – 15/15 testų (pakartojus dėl nuo laiko priklausančio testo), v0.3 – 19/19. Našumas matuotas dviejuose JVM procesuose, kiekviename po 3 apšilimo ir 5 matuojamus paleidimus. Lentelėje pateikti atskirų operacijų, ne visos simuliacijos, rezultatai.

## Projekto struktūra

```text
src/
├── main/java/lt/vu/blockchain/
│   ├── Main.java                     # Paleidimas ir rezultatų išvedimas
│   ├── hashing/
│   │   └── HashFunctionV012.java     # Maišos funkcija iš pirmojo darbo
│   ├── model/
│   │   ├── User.java                 # Vartotojo duomenys
│   │   ├── Transaction.java          # Transakcijos duomenys
│   │   ├── Block.java                # Blokas ir jo transakcijos
│   │   ├── BlockHeader.java          # Bloko antraštė
│   │   ├── MiningResult.java         # Kasimo rezultatas
│   │   └── RejectedTransaction.java  # Atmesta transakcija ir priežastis
│   └── service/
│       ├── DataGenerator.java        # Duomenų generavimas
│       ├── TransactionPool.java      # Laukiančių transakcijų atranka
│       ├── Blockchain.java           # Grandinė, validacija ir balansai
│       ├── BlockchainSimulation.java # Kandidatų konkurencija
│       ├── Miner.java                # PoW kasimas
│       └── MerkleTree.java           # Merkle šaknies skaičiavimas
└── test/java/lt/vu/blockchain/service/
    ├── MerkleTreeTest.java            # Merkle medžio testai
    ├── ValidationTest.java            # Validacijos ir balansų testai
    ├── BlockchainSimulationTest.java  # Kandidatų ir PoW testai
    ├── VersionComparisonTest.java     # v0.2 ir v0.3 rezultatų palyginimas
    ├── TransactionPoolComparisonTest.java # Atrankos testai
    ├── TransactionPoolV02Baseline.java       # v0.2 atrankos kopija
    ├── MerkleTreeV02Baseline.java     # v0.2 Merkle medžio kopija
    └── PerformanceComparisonBenchmark.java # Našumo palyginimas
```

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

Našumo palyginimas po testų kompiliavimo (dvitaškis klasės kelyje tinka „macOS“ ir „Linux“):

```bash
java -Xms512m -Xmx512m -cp target/classes:target/test-classes lt.vu.blockchain.service.PerformanceComparisonBenchmark
```

## Rezultatai

### v0.2 bloko pavyzdys

Ankstesnio paleidimo pirmojo bloko duomenys (konsolės laukų pavadinimai išversti):

| Laukas | Reikšmė |
| --- | --- |
| Aukštis ir transakcijų skaičius | 1 ir 100 |
| Ankstesnio bloko maiša | `55a0d7374561c65c59ffdabe632e1a1436a9240442a6dbc5ece24a8330c9a54c` |
| Merkle šaknis | `66e707c9d5a4570a16a171abcc3248284469a3112303b796caadab6f4f4c2d9d` |
| Bloko maiša | `000e48aee1a3bef260ff71621488b49df52fee0007e503c23eaee212558936c2` |
| PoW sudėtingumas ir `nonce` | 3 ir 2934 |

| Pirmojo raundo kandidatas | Bandymų | Trukmė | PoW |
| --- | ---: | ---: | --- |
| 1 (laimėtojas) | 2 935 | 9 ms | Rastas |
| 2 | 5 000 | 14 ms | Nerastas |
| 3 | 5 000 | 14 ms | Nerastas |
| 4 | 4 702 | 13 ms | Rastas |
| 5 | 3 119 | 10 ms | Rastas |

### Galutinės suvestinės

| Rodiklis | v0.2 ankstesnis paleidimas | v0.3 patikrintas paleidimas |
| --- | ---: | ---: |
| Sugeneruota vartotojų | 1 000 | 1 000 |
| Sugeneruota transakcijų | 10 000 | 10 000 |
| Priimta blokų be genesis | 100 | 100 |
| Patvirtinta transakcijų | 9 950 | 9 943 |
| Atmesta transakcijų | 0 | 0 |
| Liko laukiančių transakcijų | 50 | 57 |
| Pradinė bendra balansų suma | 498 302 330 | 498 302 330 |
| Galutinė bendra balansų suma | 498 302 330 | 498 302 330 |
| Bendras vykdymo laikas | 5 078 ms | 4 770 ms |

v0.2 iš 10 000 transakcijų patvirtino 9 950 (99,5 %); likusios 50 laukė dėl nepakankamo siuntėjų balanso. v0.3 patvirtintų, atmestų ir laukiančių transakcijų suma taip pat lygi 10 000. Abiejų paleidimų pradinė ir galutinė balansų suma sutampa. Laiko reikšmės gautos iš atskirų, nekontroliuotų simuliacijų, todėl iš jų nedarome išvados apie visos programos pagreitėjimą. Atskiri paleidimai nėra deterministinis patvirtintų transakcijų skaičiaus palyginimas; algoritmų atitikimas tikrintas vienodų įvesčių testais. Laiko žymos ir blokų maišos tarp paleidimų gali skirtis.

## Išvada

`TransactionPool` atranka pagreitėjo **68–71 %**, o `MerkleTree` alokacijos sumažėjo **3,1–3,3 %**. Testai patvirtino rezultatų atitikimą v0.2; originali maišos funkcija liko nepakeista.
