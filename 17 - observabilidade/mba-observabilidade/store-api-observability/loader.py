import json
import http.client
from random import randrange, randint

def request(method: str, path: str, payload: str = None):
    conn = http.client.HTTPConnection("localhost:8080")
    conn.request(method, path, payload, headers = {
        'Content-Type': "application/json",
        'x-trace-id': "ead37b0f-e401-4536-a770-527394c5379d"
    })
    res = conn.getresponse()
    print(f'{path} - {res.status}')

for i in range(1, 100000):
    
    for j in range(randrange(5)):
        payload = {
            'items': [
                {
                    'product': {'id': 1},
                    'quantity': randint(0, i)
                },
                {
                    'product': {'id': 2},
                    'quantity': randint(0, i)
                }
            ]
        }
        request("POST", "/orders", json.dumps(payload))

    for j in range(randrange(5)):
        request("GET", "/orders")

    for j in range(randrange(5)):
        request("GET", f"/orders/{randrange(i)}")

    for j in range(randrange(5)):
        request("DELETE", f"/orders/{randrange(i)}/cancel")

    for j in range(randrange(5)):
        request("PUT", f"/orders/{randrange(i)}/complete")
