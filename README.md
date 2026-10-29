
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

## Rezultatai

Konsolėje pateikiama kiekvieno iškasto bloko informacija ir galutinė simuliacijos statistika.

**Bloko pavyzdys:** [įkelti išvestį]

**Galutinė suvestinė:** [įkelti išvestį]

## DI naudojimas

v0.1 kūrimui naudoti ChatGPT – planavimui ir kodo peržiūrai.
