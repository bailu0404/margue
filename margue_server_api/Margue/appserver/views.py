from django.shortcuts import render
from django.http import JsonResponse, HttpResponse
from appserver.models import *
from django.core.paginator import Paginator, EmptyPage, PageNotAnInteger
from django.utils import timezone
import random
from django.db import DatabaseError
import json
from appserver.acc_views import *
from django.contrib.auth.models import User
from django.db import IntegrityError
from django.contrib.auth.decorators import login_required



def pageIt(objList, page, pageSize = 20):
    #handle the list, return the requested page
    paginator = Paginator(objList, pageSize)
    
    try:
        pagedObject = paginator.page(page)

    except PageNotAnInteger:
        pagedObject = paginator.page(1)

    except EmptyPage:
        pagedObject = paginator.page(paginator.num_pages)

    return pagedObject

def sortIt(objList, sort):
    
    returnList = []

    if sort == 'TIMELINE':
        returnList = sorted(objList, key=lambda k: k['timestamp'], reverse=True)
    elif sort == 'TREND':
        returnList = sorted(objList, key=lambda k: k['like'], reverse=True)
    else:
        returnList = objList

    return returnList


# @login_required
def loadTopics(request):

    if request.method == "GET":

        #retrieve data from POST Body

        data = json.loads(request.read().decode("utf-8"))

        tag = data['tag']
        userID = data['userID']
        pageSize = data['size']
        page = data['page']
        lastUpdate = data['lastUpdate']
        oldestUpdate = data['oldestUpdate']
        sort = data['sort']     #about sorting, define the type of sorting and hard code in

        #get topics from DB
        if tag != '':
        #TODO algo to filter topics
            topics = Topic.objects.filter(tag__title__exact = tag)
        else:
            topics = Topic.objects.all()

        #create specific page for users to request
        pagedTopics = pageIt(topics, page, pageSize)

        response_dict = {}
        response_data = []
        
        for topic in pagedTopics:
            record = {'id': topic.id, 
                      'tag': topic.tag.title, 
                      'prop': topic.prop, 
                      'oppo': topic.oppo, 
                      'like': topic.like,
                      'timestamp': topic.timestamp,
                      }
            
            response_data.append(record)

        response_data = sortIt(response_data, sort)
        response_dict['loadTopics'] = response_data

        
        return JsonResponse(response_dict)


    else:
        return HttpResponse('POST Requested')



def loadTags(request):
    
    #if request.method == "POST":
        #retrieve data from POST Body
    #data = json.loads(request.read().decode("utf-8"))
    
    #userID = data['userID']
    pageSize = 20
    page = 1
    lastUpdate = ''
    #oldestUpdate = data['oldestUpdate']
    sort = 'TIMELINE'     #about sorting, define the type of sorting and hard code in


    #get topics from DB
    #TODO figure out the algorithm to filter tags
    if lastUpdate != '':
        tags = Tag.objects.all()
    else:
        tags = Tag.objects.all()


    #create specific page for users to request
    pagedTags = pageIt(tags, page, pageSize)

    response_dict = {}
    response_data = []
    
    for tag in pagedTags:
        record = {'title': tag.title, 
                  'like': tag.like, 
                  'timestamp': tag.timestamp,
                  }

        response_data.append(record)

    response_data = sortIt(response_data, sort)
    response_dict['loadTags'] = response_data

    return JsonResponse(response_dict)
    
    #else:
    #    return HttpResponse('Please Use POST Request')




def loadFeeds(request):
    
    if request.method == "POST":
        #retrieve data from POST Body
        data = json.loads(request.read().decode("utf-8"))
        
        userID = data['userID']
        pageSize = data['size']
        page = data['page']
        lastUpdate = data['lastUpdate']
        oldestUpdate = data['oldestUpdate']


        #Margue app send a request to pull the feeds (specific user)
        #TODO check algo
        #userList = 
        feeds = Feed.objects.filter(user = User.objects.get(pk=userID)).order_by('timestamp').reverse()

        #create specific page for users to request
        pagedFeeds = pageIt(feeds, page, pageSize)

        response_dict = {}
        response_data = []

        for feed in pagedFeeds:
            record = {'userID': feed.user.id,
                      'userName': feed.user.name,
                      'topic': feed.topicID.prop,
                      'topicID': feed.topicID.id,                       
                      'roomID': feed.room.id,
                      'timestamp': feed.timestamp, 
                      'status': feed.status,
                      'id': feed.id,
                      }

            response_data.append(record)

        response_dict['loadFeeds'] = response_data

        return JsonResponse(response_dict)
    else:
        return HttpResponse('Please Use POST Request')



##### CREATE TAG|TOPIC|ROOM #####
@login_required
def createTag(request):
    #Margue app send tag & userID, return JSON including status: success/fail
    if request.method == 'POST':
        
        response_dict = {}
        
        #retrieve data from POST Body
        data = json.loads(request.read().decode("utf-8"))
        
        userID = data['userID']
        tagTitle = data['title']

        user = User.objects.get(pk=userID)
        try:
            tag = Tag(title = tagTitle, createdBy = user)
            tag.save()
            response_dict['status'] = True
        
        except DatabaseError:
            response_dict['status'] = False

        return JsonResponse(response_dict)
    
    else:
        return HttpResponse('Please Use POST Request')


def createTopic(request):

    if request.method == 'POST':

        response_dict = {}

        #retrieve data from POST body
        data = json.loads(request.read().decode("utf-8"))
        
        userID = data['userID']
        tagTitle = data['tag']
        desc_CT = data['description']
        prop_CT = data['prop']
        oppo_CT = data['oppo']

        user_CT = User.objects.get(pk=userID)
        tag_CT = Tag.objects.get(pk=tagTitle)

        try:
            topic = Topic(tag = tag_CT, prop = prop_CT, oppo = oppo_CT, description = desc_CT)
            topic.save()
            response_dict['status'] = True
            response_dict['topicID'] = topic.id

        except DatabaseError:
            response_dict['status'] = False

        return JsonResponse(response_dict)

    else:
        return HttpResponse('Please Use POST Request')


#def createFeed(request):
#
#    if request.method == 'POST':
#
#        response_dict = {}
#
#        userID = data['userID']
#        tagTitle = request.
#        


##### LIKE TAG|TOPIC #####

def likeTag(request):

    if request.method == 'POST':

        data = json.loads(request.read().decode("utf-8"))        

        response_dict = {}
        response_data = []

        userID = data['userID']
        title = data['tagID']

        user_LT = User.objects.get(pk=userID)
        tag_LT = Tag.objects.get(pk=title)

        try:
            tagLike = TagLike(likedBy = user_LT, liked = tag_LT)
            tagLike.save()
            tag_LT.like += 1
            tag_LT.save()
            response_dict['status'] = True

        except:
            response_dict['status'] = False

        record = {'title': tag_LT.title,
                  'like': tag_LT.like,
                  'timestamp': tag_LT.timestamp,
                  }
        response_data.append(record)
        response_dict['item'] = response_data

        return JsonResponse(response_dict)

    else:
        return HttpResponse('Please Use POST Request')

def likeTopic(request):

    if request.method == 'POST':

        response_dict = {}
        response_data = []

        data = json.loads(request.read().decode("utf-8"))        

        userID = data['userID']
        topicID = data['topicID']

        user_LT = User.objects.get(pk=userID)
        topic_LT = Topic.objects.get(pk=topicID)

        try:
            topicLike = TopicLike(likedBy = user_LT, liked = topic_LT)
            topicLike.save()
            topic_LT.like += 1
            topic_LT.save()
            response_dict['status'] = True

        except:
            response_dict['status'] = False

        record = {'id': topic_LT.id,
                  'tag': topic_LT.tag.title,
                  'prop': topic_LT.prop, 
                  'oppo': topic_LT.oppo, 
                  'like': topic_LT.like,
                  }
        response_data.append(record)
        response_dict['item'] = response_data

        return JsonResponse(response_dict)

    else:
        return HttpResponse('Please Use POST Request')

##### FOLLOW #####

def follow(request):

    if request.method == 'POST':

        response_dict ={}

        data = json.loads(request.read().decode("utf-8"))        

        followerID = data['followerID']
        userID = data['userID']

        followedBy_FL = User.objects.get(pk=followerID)
        user_FL = User.objects.get(pk=userID)

        try:
            follow = Follow(followedBy = followedBy_FL, user = user_FL)
            follow.save()
            response_dict['status'] = True

        except:
            response_dict['status'] = False

        return JsonResponse(response_dict)

    else:
        return HttpResponse('Please Use POST Request')

##### CREATE DB Data for TESTING #####
def createUsers(request):
    for i in range(200):
        name = str(random.randint(10000000,99999999))
        data = {}
        data['uid'] = name
        sign_up(data)
    return HttpResponse('200 users created')

def createTags(request):
    for i in range(200):
        temp = 'tag ' + str(random.randint(100,900))
        try:
            test = Tag.objects.get(tag=temp)
            if test is not None:
                print('NOT NONE')
                continue
            else:
                tag = Tag(
                    tag = temp,
                    created_by = User.objects.get(pk=1),
                    )
                tag.save()
        except:
            print('exception catched')
            tag = Tag(
                tag = temp,
                created_by = User.objects.get(pk=1),
                    )
            tag.save()

    return HttpResponse('200 tags created')

def createTopics(request):
    for i in range(200):
        topic = Topic(
                description = 'description ' + str(i),
                prop = 'prop ' + str(i),
                oppo = 'oppo ' + str(i),
                )
        topic.save()
    return HttpResponse('200 topics created')

def createRooms(request):
    for i in range(200):
        room = Room(
                topicID = Topic.objects.all().order_by('?')[0],
                created_by = User.objects.get(pk=1),
                )
        room.save()
    return HttpResponse('200 rooms created')

def createFeeds(request):
    for i in range(200):
        feedRoom = Room.objects.all().order_by('?')[0]
        feed = Feed(
                user = User.objects.get(pk=1),
                room = feedRoom,
                topicID = feedRoom.topicID,
                status = random.choice('wl'),
                )
        feed.save()
    return HttpResponse('200 feeds created')
