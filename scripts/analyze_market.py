import pandas as pd
import matplotlib.pyplot as plt
import requests
import io

def fetch_data(url):
    print(f"Fetching data from {url}...")
    try:
        response = requests.get(url)
        response.raise_for_status()
        return pd.read_csv(io.StringIO(response.text))
    except Exception as e:
        print(f"Error fetching data: {e}")
        return None

def analyze_and_plot(df):
    if df is None or df.empty:
        print("No data to analyze.")
        return

    print("Analyzing data...")
    # Ensure timestamp is datetime
    df['timestamp'] = pd.to_datetime(df['timestamp'])
    df = df.sort_values('timestamp')

    # Calculate VWAP (approximate) or just Price Series
    
    plt.figure(figsize=(12, 8))

    # Subplot 1: Price
    plt.subplot(2, 1, 1)
    plt.plot(df['timestamp'], df['price'], marker='o', linestyle='-', markersize=2, label='Trade Price')
    plt.title('Trade Price History')
    plt.ylabel('Price')
    plt.grid(True)
    plt.legend()

    # Subplot 2: Volume
    plt.subplot(2, 1, 2)
    plt.bar(df['timestamp'], df['quantity'], width=0.0001, color='orange', label='Volume') # Width is tricky with time
    plt.title('Trade Volume')
    plt.ylabel('Quantity')
    plt.xlabel('Time')
    plt.grid(True)
    plt.legend()

    plt.tight_layout()
    output_file = 'analytics_report.png'
    plt.savefig(output_file)
    print(f"Report saved to {output_file}")

if __name__ == "__main__":
    API_URL = "http://localhost:8080/api/analytics/trades/csv"
    
    # 1. Fetch
    df = fetch_data(API_URL)
    
    # 2. Analyze
    if df is not None:
        print(f"Fetched {len(df)} trades.")
        print(df.head())
        analyze_and_plot(df)
