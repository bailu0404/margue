# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations
import datetime
from django.utils.timezone import utc


class Migration(migrations.Migration):

    dependencies = [
        ('appserver', '0001_initial'),
    ]

    operations = [
        migrations.AlterField(
            model_name='feed',
            name='created_time',
            field=models.DateTimeField(default=datetime.datetime(2015, 5, 17, 7, 9, 45, 975416, tzinfo=utc)),
            preserve_default=True,
        ),
        migrations.AlterField(
            model_name='follow',
            name='created_time',
            field=models.DateTimeField(default=datetime.datetime(2015, 5, 17, 7, 9, 45, 977893, tzinfo=utc)),
            preserve_default=True,
        ),
        migrations.AlterField(
            model_name='room',
            name='created_time',
            field=models.DateTimeField(default=datetime.datetime(2015, 5, 17, 7, 9, 45, 974643, tzinfo=utc)),
            preserve_default=True,
        ),
        migrations.AlterField(
            model_name='tag',
            name='created_time',
            field=models.DateTimeField(default=datetime.datetime(2015, 5, 17, 7, 9, 45, 971673, tzinfo=utc)),
            preserve_default=True,
        ),
        migrations.AlterField(
            model_name='tag_like',
            name='created_time',
            field=models.DateTimeField(default=datetime.datetime(2015, 5, 17, 7, 9, 45, 976344, tzinfo=utc)),
            preserve_default=True,
        ),
        migrations.AlterField(
            model_name='topic',
            name='created_time',
            field=models.DateTimeField(default=datetime.datetime(2015, 5, 17, 7, 9, 45, 972924, tzinfo=utc)),
            preserve_default=True,
        ),
        migrations.AlterField(
            model_name='topic_like',
            name='created_time',
            field=models.DateTimeField(default=datetime.datetime(2015, 5, 17, 7, 9, 45, 977170, tzinfo=utc)),
            preserve_default=True,
        ),
    ]
