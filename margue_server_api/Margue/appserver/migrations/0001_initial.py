# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations
from django.conf import settings
from django.utils.timezone import utc
import datetime


class Migration(migrations.Migration):

    dependencies = [
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.CreateModel(
            name='Feed',
            fields=[
                ('id', models.AutoField(verbose_name='ID', auto_created=True, primary_key=True, serialize=False)),
                ('created_time', models.DateTimeField(default=datetime.datetime(2015, 4, 23, 7, 40, 56, 688482, tzinfo=utc))),
                ('status', models.CharField(default='m', max_length=1)),
            ],
            options={
            },
            bases=(models.Model,),
        ),
        migrations.CreateModel(
            name='Follow',
            fields=[
                ('id', models.AutoField(verbose_name='ID', auto_created=True, primary_key=True, serialize=False)),
                ('created_time', models.DateTimeField(default=datetime.datetime(2015, 4, 23, 7, 40, 56, 690797, tzinfo=utc))),
                ('be_followed', models.ForeignKey(to=settings.AUTH_USER_MODEL)),
                ('followed_by', models.ForeignKey(related_name='follower', to=settings.AUTH_USER_MODEL)),
            ],
            options={
            },
            bases=(models.Model,),
        ),
        migrations.CreateModel(
            name='Room',
            fields=[
                ('id', models.AutoField(verbose_name='ID', auto_created=True, primary_key=True, serialize=False)),
                ('created_time', models.DateTimeField(default=datetime.datetime(2015, 4, 23, 7, 40, 56, 687746, tzinfo=utc))),
                ('created_by', models.ForeignKey(to=settings.AUTH_USER_MODEL)),
            ],
            options={
            },
            bases=(models.Model,),
        ),
        migrations.CreateModel(
            name='Tag',
            fields=[
                ('id', models.AutoField(verbose_name='ID', auto_created=True, primary_key=True, serialize=False)),
                ('tag', models.CharField(default='None', max_length=50, unique=True)),
                ('like', models.IntegerField(default=0)),
                ('created_time', models.DateTimeField(default=datetime.datetime(2015, 4, 23, 7, 40, 56, 684892, tzinfo=utc))),
                ('created_by', models.ForeignKey(to=settings.AUTH_USER_MODEL)),
            ],
            options={
            },
            bases=(models.Model,),
        ),
        migrations.CreateModel(
            name='Tag_Like',
            fields=[
                ('id', models.AutoField(verbose_name='ID', auto_created=True, primary_key=True, serialize=False)),
                ('created_time', models.DateTimeField(default=datetime.datetime(2015, 4, 23, 7, 40, 56, 689287, tzinfo=utc))),
                ('liked_by', models.ForeignKey(to=settings.AUTH_USER_MODEL)),
                ('tag', models.ForeignKey(to='appserver.Tag')),
            ],
            options={
            },
            bases=(models.Model,),
        ),
        migrations.CreateModel(
            name='Topic',
            fields=[
                ('id', models.AutoField(verbose_name='ID', auto_created=True, primary_key=True, serialize=False)),
                ('description', models.TextField(blank=True)),
                ('prop', models.CharField(max_length=200)),
                ('oppo', models.CharField(max_length=200)),
                ('like', models.IntegerField(default=0)),
                ('created_time', models.DateTimeField(default=datetime.datetime(2015, 4, 23, 7, 40, 56, 686131, tzinfo=utc))),
                ('tag', models.ManyToManyField(to='appserver.Tag')),
            ],
            options={
            },
            bases=(models.Model,),
        ),
        migrations.CreateModel(
            name='Topic_Like',
            fields=[
                ('id', models.AutoField(verbose_name='ID', auto_created=True, primary_key=True, serialize=False)),
                ('created_time', models.DateTimeField(default=datetime.datetime(2015, 4, 23, 7, 40, 56, 690105, tzinfo=utc))),
                ('liked_by', models.ForeignKey(to=settings.AUTH_USER_MODEL)),
                ('topic', models.ForeignKey(to='appserver.Topic')),
            ],
            options={
            },
            bases=(models.Model,),
        ),
        migrations.CreateModel(
            name='Weibo_Profile',
            fields=[
                ('id', models.BigIntegerField(primary_key=True, serialize=False)),
                ('idstr', models.CharField(max_length=50, blank=True)),
                ('screen_name', models.CharField(max_length=50, blank=True)),
                ('name', models.CharField(max_length=50, blank=True)),
                ('province', models.IntegerField(null=True)),
                ('city', models.IntegerField(null=True)),
                ('location', models.CharField(max_length=50, blank=True)),
                ('description', models.CharField(max_length=200, blank=True)),
                ('url', models.URLField(blank=True)),
                ('profile_image_url', models.URLField(blank=True)),
                ('profile_url', models.URLField(blank=True)),
                ('domain', models.CharField(max_length=50, blank=True)),
                ('weihao', models.CharField(max_length=50, blank=True)),
                ('gender', models.CharField(max_length=2, blank=True)),
                ('followers_count', models.IntegerField(null=True)),
                ('friends_count', models.IntegerField(null=True)),
                ('statuses_count', models.IntegerField(null=True)),
                ('favourites_count', models.IntegerField(null=True)),
                ('created_at', models.CharField(max_length=50, blank=True)),
                ('following', models.NullBooleanField()),
                ('allow_all_act_msg', models.NullBooleanField()),
                ('geo_enabled', models.NullBooleanField()),
                ('verified', models.NullBooleanField()),
                ('verified_type', models.IntegerField(null=True)),
                ('remark', models.CharField(max_length=200, blank=True)),
                ('allow_all_comment', models.NullBooleanField()),
                ('avatar_large', models.URLField(blank=True)),
                ('avatar_hd', models.URLField(blank=True)),
                ('verified_reason', models.CharField(max_length=50, blank=True)),
                ('follow_me', models.NullBooleanField()),
                ('online_status', models.IntegerField(null=True)),
                ('bi_followers_count', models.IntegerField(null=True)),
                ('lang', models.CharField(max_length=50, blank=True)),
                ('user', models.OneToOneField(to=settings.AUTH_USER_MODEL)),
            ],
            options={
            },
            bases=(models.Model,),
        ),
        migrations.AddField(
            model_name='room',
            name='topic',
            field=models.ForeignKey(to='appserver.Topic'),
            preserve_default=True,
        ),
        migrations.AddField(
            model_name='feed',
            name='room',
            field=models.ForeignKey(to='appserver.Room'),
            preserve_default=True,
        ),
        migrations.AddField(
            model_name='feed',
            name='topic',
            field=models.ForeignKey(to='appserver.Topic'),
            preserve_default=True,
        ),
        migrations.AddField(
            model_name='feed',
            name='user',
            field=models.ForeignKey(to=settings.AUTH_USER_MODEL),
            preserve_default=True,
        ),
    ]
