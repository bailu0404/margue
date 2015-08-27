# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
    ]

    operations = [
        migrations.CreateModel(
            name='Room',
            fields=[
                ('id', models.AutoField(auto_created=True, serialize=False, verbose_name='ID', primary_key=True)),
                ('p1', models.CharField(max_length=50, blank=True)),
                ('p2', models.CharField(max_length=50, blank=True)),
                ('p3', models.CharField(max_length=50, blank=True)),
                ('p4', models.CharField(max_length=50, blank=True)),
                ('o1', models.CharField(max_length=50, blank=True)),
                ('o2', models.CharField(max_length=50, blank=True)),
                ('o3', models.CharField(max_length=50, blank=True)),
                ('o4', models.CharField(max_length=50, blank=True)),
                ('speaker', models.CharField(max_length=50, blank=True)),
            ],
            options={
            },
            bases=(models.Model,),
        ),
    ]
