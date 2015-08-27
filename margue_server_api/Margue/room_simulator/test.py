__author__ = 'Zach'

import json
import urllib.request
import time

url = 'http://52.74.171.135:8080/room_simulator/refresh/'
newConditions = {"position": 'p3', "name":'2b'}
params = json.dumps(newConditions).encode('utf8')
req = urllib.request.Request(url, data=params,
                             headers={'content-type': 'application/json'})
response = urllib.request.urlopen(req)

print(response.read().decode('utf8'))
