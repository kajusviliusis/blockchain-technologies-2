
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

### Bloko pavyzdys

```text
Block height: 100, transactions: 48
  Previous hash: 000cae7f651815c656c57de46b6859ac9a5ab94d6f5f42b8cf7aab449ce97d6a
  Transaction root: c52c7b728b860cbe4a61d9e3c37d5ea8f6c79633af7c94330a245bc78b945631
  Block hash: 000f2f4ec833eca97b667fa43365d7beac11e425d6cf59efd34da8761968699f
  Difficulty: 3, nonce: 9987
  Mining attempts: 9988, duration: 33 ms
```

### Galutinė suvestinė

```text
Generated users: 1000
Generated transactions: 10000
Accepted non-genesis blocks: 100
Confirmed transactions: 9948
Rejected transactions: 0
Remaining pending transactions: 52
Initial total balance: 498302330
Final total balance: 498302330
Total execution time: 1822 ms
```

Iš 10 000 sugeneruotų transakcijų patvirtintos 9 948 (99,48 %).
Likusios 52 transakcijos neįvykdytos dėl nepakankamo siuntėjų balanso.

Pradinė ir galutinė lėšų suma sutampa, todėl simuliacijoje išlaikytas lėšų sumos pastovumas.

## DI naudojimas

v0.1 kūrimui naudoti ChatGPT – planavimui ir kodo peržiūrai.
