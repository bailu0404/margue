from django.db import models
from django.utils import timezone
from Margue import settings
from django.contrib.auth.models import User


# class User(models.Model):
# name = models.CharField(max_length=50)
#     password = models.CharField(max_length=50)
#     registered_on = models.DateTimeField('Registered on', default=timezone.now())
#
#     def __str__(self):
#         return self.name
#
#
# class Avatar(models.Model):
#     user = models.ForeignKey('django.contrib.auth.User')
#     imgfile = models.ImagesField(upload_to=settings.MEDIA_ROOT)


class Weibo_Profile(models.Model):
    user = models.OneToOneField(settings.AUTH_USER_MODEL)
    id = models.BigIntegerField(primary_key=True)  # weibo UID
    idstr = models.CharField(max_length=50, blank=True)  # UID in String
    screen_name = models.CharField(max_length=50, blank=True)  # nickname
    name = models.CharField(max_length=50, blank=True)  # displayed name

    province = models.IntegerField(null=True)  # province ID
    city = models.IntegerField(null=True)  # city ID

    location = models.CharField(max_length=50, blank=True)  # user location
    description = models.CharField(max_length=200, blank=True)  # personal desript

    url = models.URLField(blank=True)  # blog url
    profile_image_url = models.URLField(blank=True)  # 50*50 avatar
    profile_url = models.URLField(blank=True)  # weibo url

    domain = models.CharField(max_length=50, blank=True)  # custom url
    weihao = models.CharField(max_length=50, blank=True)  # 微号
    gender = models.CharField(max_length=2, blank=True)  # m/f/n:unknown

    followers_count = models.IntegerField(null=True)  #
    friends_count = models.IntegerField(null=True)  #
    statuses_count = models.IntegerField(null=True)  # weibo count
    favourites_count = models.IntegerField(null=True)  #

    created_at = models.CharField(max_length=50, blank=True)  # account create time

    following = models.NullBooleanField()  # NOT IN USE
    allow_all_act_msg = models.NullBooleanField()  # allow stranger message me?
    geo_enabled = models.NullBooleanField()  # allow location mark?
    verified = models.NullBooleanField()  # Vip?

    verified_type = models.IntegerField(null=True)  # NOT IN USE

    remark = models.CharField(max_length=200, blank=True)  # for relationship check

    allow_all_comment = models.NullBooleanField()  # allow stranger to comment?

    avatar_large = models.URLField(blank=True)  # 180*180 avatar url
    avatar_hd = models.URLField(blank=True)  # HD avatar url
    verified_reason = models.CharField(max_length=50, blank=True)  # reason for verification

    follow_me = models.NullBooleanField()  # relationship with me (token)

    online_status = models.IntegerField(null=True)  # 1 online, 0 offline
    bi_followers_count = models.IntegerField(null=True)  # mutual following?
    lang = models.CharField(max_length=50, blank=True)  # zh-cn/zh-tw/en

    def __str__(self):
        return self.name


class Tag(models.Model):
    tag = models.CharField(max_length=50, default='None', unique=True)
    like = models.IntegerField(default=0)
    created_by = models.ForeignKey(settings.AUTH_USER_MODEL)
    created_time = models.DateTimeField(default=timezone.now())

    def return_like(self):
        return self.like

    def __str__(self):
        return self.tag


class Topic(models.Model):
    tag = models.ManyToManyField('Tag')
    description = models.TextField(blank=True)
    prop = models.CharField(max_length=200)
    oppo = models.CharField(max_length=200)
    like = models.IntegerField(default=0)
    created_time = models.DateTimeField(default=timezone.now())


    def return_like(self):
        return self.like

    def __str__(self):
        return self.prop


class Room(models.Model):
    topic = models.ForeignKey('Topic')
    created_by = models.ForeignKey(settings.AUTH_USER_MODEL)
    created_time = models.DateTimeField(default=timezone.now())

    def __str__(self):
        return 'Room ID ' + str(self.id)

    def returnTopic(self):
        return self.topic


class Feed(models.Model):
    user = models.ForeignKey(settings.AUTH_USER_MODEL)
    topic = models.ForeignKey('Topic')
    room = models.ForeignKey('Room')
    created_time = models.DateTimeField(default=timezone.now())

    #describe the status of the user currently
    #l = listening, m = marguing , c = completed
    status = models.CharField(max_length=1, default='m')

    def __str__(self):
        return 'Feed ID ' + str(self.id)

    def returnTopic(self):
        return self.topic


#table of records that tag been liked by user
class Tag_Like(models.Model):
    tag = models.ForeignKey('Tag')
    liked_by = models.ForeignKey(settings.AUTH_USER_MODEL)
    created_time = models.DateTimeField(default=timezone.now())


#table of records that topic been liked by user
class Topic_Like(models.Model):
    topic = models.ForeignKey('Topic')
    liked_by = models.ForeignKey(settings.AUTH_USER_MODEL)
    created_time = models.DateTimeField(default=timezone.now())


#table of records of users social status
class Follow(models.Model):
    be_followed = models.ForeignKey(settings.AUTH_USER_MODEL)
    followed_by = models.ForeignKey(settings.AUTH_USER_MODEL, related_name='follower')
    created_time = models.DateTimeField(default=timezone.now())


    
 
