from django.conf.urls import patterns, url
from room_simulator import views


urlpatterns = patterns('',


        url(r'^enter/$', views.enter, name='enter'),
        url(r'^start/$', views.start, name='start'),
        url(r'^refresh/$', views.refresh, name='refresh'),
        url(r'^drop/$', views.drop, name='drop'),
        url(r'^quit/$', views.quit, name='quit'),
        url(r'^reset/$', views.reset, name='reset'),


)
