import json
import requests

url_source = 'https://dummyjson.com/products?limit=200'
url_api = 'http://localhost:8081/products'

sample_products = requests.request("GET", url_source).json()
for p in sample_products['products']:
    print(p['id'])
    obj = {
      'category': p['category'],
      'title': p['title'],
      'description': p['description'],
      'price': p['price'],
      'rating': p['rating'],
      'brand': None if 'brand' not in p else p['brand']
    }
    result = requests.post(url_api, json=p)
    print(result)