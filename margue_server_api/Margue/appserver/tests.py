from django.test import TestCase, Client
from appserver.models import User
import json

# Create your tests here.

class TestRunDown(TestCase):
    
    def setUp(self):
        self.c = Client()

    def testRunDown(self):
        #test loadTopics
        #data = {
        #        'tag':'',
        #        'userID':1,
        #        'size':10,
        #        'page':2,
        #        'lastUpdate':'last',
        #        'oldestUpdate':'oldest',
        #        'sort':'r',
        #        }

        #print('test data is ' + json.dumps(data))
        #
        #response = self.c.post('/appserver/loadTopics/', data)
        #print('\n\n\n -------------------------TEST LOADTOPICS ------------------------------------------------\n')
        #print(response.content)

        #test loadTags
        #dataTag = {
        #        'userID': 1,
        #        'size' : 30,
        #        'page' : 2,
        #        'lastUpdate' : '',
        #        'oldestUpdate' : '',
        #        'sort' : '',
        #        }
        #response = self.c.post('/appserver/loadTags/',dataTag)
        #print('\n\n\n -------------------------TEST LOADTAGS --------------------------------------------------\n')
        #print(response.content)


        ##test loadFeeds
        #dataFeed = {
        #        'userID':1,
        #        'size':30,
        #        'page':2,
        #        'lastUpdate':'',
        #        'oldestUpdate':'',
        #    }
        #response = self.c.post('/appserver/loadFeeds/', dataFeed)
        #print('\n\n\n -------------------------TEST LOADFEEDS --------------------------------------------------\n')
        #print(response.content)

        #test createUser
        response = self.c.post('/appserver/account/log_out/')
        print('\n\n\n -------------------------TEST Log OUTTTTTT --------------------------------------------------\n')
        print(response.content)

        data = json.dumps(
            {
                'uid': 'uid_value',
                'access_token': 'access_token_value',
                'refresh_token': 'refresh_token_value',
                'expire_date': 'expire_date_value',
                }
        )

        response = self.c.post('/appserver/account/log_in/', content_type='application/json',data=data)
        print('\n\n\n -------------------------TEST CREATEUSER 1 --------------------------------------------------\n')
        print(response.content)

        data = json.dumps(
            {
                'uid': 'uid_value',
                'access_token': 'access_token_value',
                'refresh_token': 'refresh_token_value',
                'expire_date': 'expire_date_value',
                }
        )

        response = self.c.post('/appserver/account/log_in/', content_type='application/json',data=data)
        print('\n\n\n -------------------------TEST again --------------------------------------------------\n')
        print(response.content)


        response = self.c.post('/appserver/account/log_out/')
        print('\n\n\n -------------------------TEST Log OUTTTTTT --------------------------------------------------\n')
        print(response.content)




        ##test createTag
        #data = {
        #        'userID': 1,
        #        'title': 'abcdabcd',
        #        }

        #response = self.c.post('/appserver/createTag/', data)
        #print('\n\n\n -------------------------TEST CREATETAG --------------------------------------------------\n')
        #print(response.content)


        ##test createTopic
        #data = {
        #        'userID': 1,
        #        'tag': 'abcdabcd',
        #        'description': 'description sucks',
        #        'prop': 'prop abcd',
        #        'oppo': 'oppo abcd',
        #        }

        #response = self.c.post('/appserver/createTopic/', data)
        #print('\n\n\n -------------------------TEST CREATETOPIC --------------------------------------------------\n')
        #print(response.content)


        ##test likeTag
        #data = {
        #        'userID': 1,
        #        'tag': 'abcdabcd',
        #         }

        #response = self.c.post('/appserver/likeTag/', data)
        #print('\n\n\n -------------------------TEST LIKETAG --------------------------------------------------\n')
        #print(response.content)

        # 
        ##test likeTopic
        #data = {
        #        'userID': 1,
        #        'topicID': 1,
        #         }

        #response = self.c.post('/appserver/likeTopic/', data)
        #print('\n\n\n -------------------------TEST LIKETOPIC --------------------------------------------------\n')
        #print(response.content)




