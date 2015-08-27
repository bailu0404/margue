# -*- coding: utf-8 -*-

from django.shortcuts import render
import json
from room_simulator.models import *
from django.http import HttpResponse, JsonResponse
# Create your views here.

def enter(request):
    if request.method == "POST":

        # retrieve data from POST Body

        try:
            data = json.loads(request.read().decode("utf-8"))

            position = data['position']
            name = data['name']

            # get topics from DB
            room = Room.objects.get(pk=1)
            if position == 'p1':
                room.p1 = name
            elif position == 'p2':
                room.p2 = name
            elif position == 'p3':
                room.p3 = name
            elif position == 'p4':
                room.p4 = name
            elif position == 'o1':
                room.o1 = name
            elif position == 'o2':
                room.o2 = name
            elif position == 'o3':
                room.o3 = name
            elif position == 'o4':
                room.o4 = name
            room.save()

            return HttpResponse(True)

        except:
            return HttpResponse(False)

    else:
        HttpResponse('GET REQUIRED')


def start(request):
    if request.method == 'POST':

        try:

            # get topics from DB
            room = Room.objects.get(pk=1)
            room.speaker = room.p1
            room.save()
            return HttpResponse(True)

        except:
            return HttpResponse(False)

    else:
        HttpResponse('GET REQUIRED')


def refresh(request):
    if request.method == 'POST':

        try:
            # get topics from DB
            room = Room.objects.get(pk=1)
            user = []
            if room.p1 != '':
                user.append({'position': 'p1', 'name': room.p1})
            if room.p2 != '':
                user.append({'position': 'p2', 'name': room.p2})
            if room.p3 != '':
                user.append({'position': 'p3', 'name': room.p3})
            if room.p4 != '':
                user.append({'position': 'p4', 'name': room.p4})
            if room.o1 != '':
                user.append({'position': 'o1', 'name': room.o1})
            if room.o2 != '':
                user.append({'position': 'o2', 'name': room.o2})
            if room.o3 != '':
                user.append({'position': 'o3', 'name': room.o3})
            if room.o4 != '':
                user.append({'position': 'o4', 'name': room.o4})
            if room.speaker != '':
                user.append({'position': 'speaker', 'name': room.speaker})

            record = {'user': user,
                      'prop': '股票会涨',
                      'oppo': '股票会跌',
            }

            return JsonResponse(record)

        except:
            return HttpResponse(False)

    else:
        try:
            # get topics from DB
            room = Room.objects.get(pk=1)
            user = []
            if room.p1 != '':
                user.append({'position': 'p1', 'name': room.p1})
            if room.p2 != '':
                user.append({'position': 'p2', 'name': room.p2})
            if room.p3 != '':
                user.append({'position': 'p3', 'name': room.p3})
            if room.p4 != '':
                user.append({'position': 'p4', 'name': room.p4})
            if room.o1 != '':
                user.append({'position': 'o1', 'name': room.o1})
            if room.o2 != '':
                user.append({'position': 'o2', 'name': room.o2})
            if room.o3 != '':
                user.append({'position': 'o3', 'name': room.o3})
            if room.o4 != '':
                user.append({'position': 'o4', 'name': room.o4})
            if room.speaker != '':
                user.append({'position': 'speaker', 'name': room.speaker})

            record = {'user': user,
                      'prop': '股票会涨',
                      'oppo': '股票会跌',
            }

            return JsonResponse(record)

        except:
            return HttpResponse(False)


def who_is_next(position):
    sequence = ['p1', 'o1', 'p2', 'o2', 'p3', 'o3', 'p4', 'o4']

    index = sequence.index(position)
    if index == 7:
        return sequence[0]
    else:
        return sequence[index + 1]


def drop(request):

    if request.method == 'POST':

        try:
            data = json.loads(request.read().decode("utf-8"))

            position = data['position']
            name = data['name']
            # get topics from DB
            room = Room.objects.get(pk=1)
            next_position = position

            next_speaker = ''


            while next_speaker == '':
                next_position = who_is_next(next_position)

                if next_position == 'p1':
                    next_speaker = room.p1
                elif next_position == 'p2':
                    next_speaker = room.p2
                elif next_position == 'p3':
                    next_speaker = room.p3
                elif next_position == 'p4':
                    next_speaker = room.p4
                elif next_position == 'o1':
                    next_speaker = room.o1
                elif next_position == 'o2':
                    next_speaker = room.o2
                elif next_position == 'o3':
                    next_speaker = room.o3
                elif next_position == 'o4':
                    next_speaker = room.o4

            room.speaker = next_speaker
            room.save()

            return HttpResponse(True)

        except:
            return HttpResponse(False)

    else:
        HttpResponse('GET REQUIRED')

def quit(request):

    if request.method == 'POST':

        try:

            data = json.loads(request.read().decode("utf-8"))

            position = data['position']
            name = data['name']
            # get topics from DB
            room = Room.objects.get(pk=1)

            if position == 'p1':
                room.p1 = ''
            elif position == 'p2':
                room.p2 = ''
            elif position == 'p3':
                room.p3 = ''
            elif position == 'p4':
                room.p4 = ''
            elif position == 'o1':
                room.o1 = ''
            elif position == 'o2':
                room.o2 = ''
            elif position == 'o3':
                room.o3 = ''
            elif position == 'o4':
                room.o4 = ''
            room.save()
            return HttpResponse(True)

        except:
            return HttpResponse(False)

    else:
        HttpResponse('GET REQUIRED')

def reset(request):

    try:
        room = Room.objects.get(pk=1)
        room.p1 = ''
        room.p2 = ''
        room.p3 = ''
        room.p4 = ''
        room.o1 = ''
        room.o2 = ''
        room.o3 = ''
        room.o4 = ''
        room.speaker = ''
        room.save()
        return HttpResponse(True)

    except:
        new_room = Room(pk=1)
        new_room.save()
        return HttpResponse('new room created')