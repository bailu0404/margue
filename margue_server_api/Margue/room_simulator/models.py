from django.db import models

# Create your models here.

class Room(models.Model):

    p1 = models.CharField(max_length=50, blank=True)
    p2 = models.CharField(max_length=50, blank=True)
    p3 = models.CharField(max_length=50, blank=True)
    p4 = models.CharField(max_length=50, blank=True)

    o1 = models.CharField(max_length=50, blank=True)
    o2 = models.CharField(max_length=50, blank=True)
    o3 = models.CharField(max_length=50, blank=True)
    o4 = models.CharField(max_length=50, blank=True)

    speaker = models.CharField(max_length=50, blank=True)

