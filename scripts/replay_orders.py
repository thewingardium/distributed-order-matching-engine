import csv
import requests
import time
import sys

API_URL = "http://localhost:8080/api/orders"
API_KEY = "test-api-key"

def replay_orders(csv_file):
    print(f"Loading orders from {csv_file}...")
    
    with open(csv_file, 'r') as f:
        reader = csv.DictReader(f)
        orders = list(reader)
        
    print(f"Found {len(orders)} orders. Starting replay...")
    
    success_count = 0
    fail_count = 0
    
    for i, row in enumerate(orders):
        payload = {
            "symbol": row['symbol'],
            "side": row['side'],
            "type": row['type'],
            "quantity": int(row['quantity'])
        }
        
        # Add price only if it exists (Limit orders)
        if row['price']:
            payload["price"] = float(row['price'])
            
        headers = {
            "Content-Type": "application/json",
            "X-API-KEY": API_KEY
        }
        
        try:
            response = requests.post(API_URL, json=payload, headers=headers)
            if response.status_code == 200:
                print(f"[{i+1}/{len(orders)}] SUCCESS: {row['side']} {row['symbol']} {row['quantity']} @ {row.get('price', 'MKT')}")
                success_count += 1
            else:
                print(f"[{i+1}/{len(orders)}] FAILED ({response.status_code}): {response.text}")
                fail_count += 1
        except Exception as e:
            print(f"[{i+1}/{len(orders)}] ERROR: {str(e)}")
            fail_count += 1
            
        # Small delay to see progress
        time.sleep(0.05)

    print("-" * 30)
    print(f"Replay Complete.")
    print(f"Success: {success_count}")
    print(f"Failed: {fail_count}")

if __name__ == "__main__":
    file_path = "data/test_orders.csv"
    if len(sys.argv) > 1:
        file_path = sys.argv[1]
    
    # Adjust path if running from scripts dir
    try:
        replay_orders(file_path)
    except FileNotFoundError:
        # Try finding it relative to root if run from scripts folder
        replay_orders("../" + file_path)
