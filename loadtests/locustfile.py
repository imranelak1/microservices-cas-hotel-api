"""Parcours utilisateur REST ; lancer séparément de la campagne comparative."""
import itertools
import json
import os
from locust import HttpUser, between, task

rooms = itertools.count(1)


class ReservationUser(HttpUser):
    wait_time = between(.1, .5)

    def on_start(self):
        self.room = 1 + (next(rooms) - 1) % 1000
        response = self.client.post('/auth/token', json={'username': 'student', 'password': 'labpass'}, name='login')
        self.client.headers['Authorization'] = 'Bearer ' + response.json()['token']

    @task
    def reservation(self):
        body = {'clientId': 1, 'chambreId': self.room, 'dateDebut': '2092-01-01',
                'dateFin': '2092-01-03', 'preferences': ''}
        body['preferences'] = 'x' * max(0, int(os.getenv('SIZE_KIB', '1')) * 1024 - len(json.dumps(body, separators=(',', ':'))))
        response = self.client.post('/api/reservations', json=body, name='create')
        if response.status_code != 201:
            return
        path = '/api/reservations/' + str(response.json()['id'])
        try:
            self.client.get(path, name='read')
            body['dateFin'] = '2092-01-04'
            self.client.put(path, json=body, name='update')
        finally:
            self.client.delete(path, name='delete')
