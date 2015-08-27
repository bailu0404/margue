from django.conf.urls import patterns, url
from appserver import views, acc_views


urlpatterns = patterns('',

        ########################## LOAD TOPIC|TAG|FEED #############################
        #ex: /appserver/loadTopics/
        url(r'^loadTopics/$', views.loadTopics, name='loadTopics'),

        #ex: /appserver/loadTags/
        url(r'^loadTags/$', views.loadTags, name='loadTags'),

        #ex: /appserver/loadFeeds/
        url(r'^loadFeeds/$', views.loadFeeds, name='loadFeeds'),


        ########################## CREATE TOPIC|TAG|FEED #############################        
        #ex: /appserver/createTag/
        url(r'^createTag/$', views.createTag, name='createTag'),


        #ex: /appserver/createTopic/
        url(r'^createTopic/$', views.createTopic, name='createTopic'),


        ########################## LIKE TOPIC|TAG #############################        
        #ex: /appserver/likeTag/
        url(r'^likeTag/$', views.likeTag, name='likeTag'),

        #ex: /appserver/likeTopic/
        url(r'^likeTopic/$', views.likeTopic, name='likeTopic'),


        ########################## SOCIAL ############################################
        #ex: /appserver/follow/
        url(r'^follow/$', views.follow, name='follow'),
        


        
        #create 200 Topics Tags and users for testing
        url(r'^createTopics/$', views.createTopics, name='createTopics'),
        url(r'^createTags/$', views.createTags, name='createTags'),
        url(r'^createUsers/$', views.createUsers, name='createUsers'),
        url(r'^createRooms/$', views.createRooms, name='createRooms'),
        url(r'^createFeeds/$', views.createFeeds, name='createFeeds'),

        ### account management ###
        url(r'^account/change_password/$', acc_views.change_password, name='change_password'),
        url(r'^account/change_email/$', acc_views.change_email, name='change_email'),
        url(r'^account/download_avatar/$', acc_views.download_avatar, name='download_avatar'),

        url(r'^account/log_in/$', acc_views.log_in, name='log_in'),
        url(r'^account/log_out/$', acc_views.log_out, name='log_out'),
        

        
        
        
        
        


)
