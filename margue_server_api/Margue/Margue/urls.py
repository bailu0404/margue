from django.conf.urls import patterns, include, url
from django.contrib import admin

urlpatterns = patterns('',
    # Examples:
    # url(r'^$', 'Margue.views.home', name='home'),
    # url(r'^blog/', include('blog.urls')),

    url(r'^admin/', include(admin.site.urls)),
    
    #views URL
    url(r'^appserver/', include('appserver.urls')),
    url(r'^room_simulator/', include('room_simulator.urls')),

    
)
