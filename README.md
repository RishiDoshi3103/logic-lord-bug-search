# CSC 345 Programming Project 1 — Logic / Lord Bug Search (Java)

This project simulates Logic’s teleport-based search for Lord Bug on a **100×100 grid** of square miles (coordinates `0..99`).  
The program reads a list of random teleport destinations (RS = Random Spot) from a text file, follows the required search pattern, tracks which squares have already been searched, and prints:

1. The **sequence of searched coordinates** (one per line)
2. The **total number of hours** taken (last line)

The program is designed to be memory-efficient by storing only the squares that were actually searched (sparse visited structure), while still allowing fast “already searched?” checks.

---

## Rules Implemented (High-Level)

- Grid bounds: `0 ≤ col,row ≤ 99`
- Center square is `(50,50)` and is considered **already searched**
- For each RS:
  - If **RS + its four neighbors (N,S,E,W)** have all been searched (or are out of bounds), skip it.
  - Otherwise, search in this order:
    1. RS
    2. North (row - 1)
    3. East  (col + 1)
    4. South (row + 1)
    5. West  (col - 1)
  - Skip any square that is out of bounds or already searched.
- Travel:
  - Moving to an adjacent square mile costs **1 hour**
  - Total travel time uses Manhattan distance
- Searching a square mile costs **2 hours**
- Logic can work up to **16 hours** before returning to sleep
- Sleeping takes **8 hours**
- If there isn’t enough time to *fully* travel + search another square before sleep, Logic sleeps early

---

## Files

- `search.java` — main program (class name: `search`)
- `input.txt` — sample input (you can create your own)

---

## Requirements

- Java (assignment uses Java 25; any modern Java should work if your environment supports it)

---

## How to Compile

From the folder containing `search.java`:

```bash
javac search.java
