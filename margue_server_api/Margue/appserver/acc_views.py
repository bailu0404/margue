from appserver.views import *
from appserver.static.weibo_api import Weibo_API
from django.http import JsonResponse, HttpResponse
from appserver.models import *
import json
from Margue import settings

from django.contrib.auth import authenticate, login, logout
from django.db import IntegrityError
from django.contrib.auth.decorators import login_required
import urllib.request





#### ACCOUNT MANAGEMENT ####
def log_in(request):
    if request.method == 'POST':
        data = json.loads(request.read().decode('utf8'))

        user = authenticate(username=data['uid'], password=settings.DEFAULT_PASSWORD)

        if user is not None:
            if user.is_active:
                login(request, user)

                return HttpResponse('LOGIN OK POST')
            else:
                return HttpResponse('USER NOT ACTIVE')

        else:
            result = sign_up(data)
            if result:
                return HttpResponse('NEW SIGN UP')
            else:
                return HttpResponse('SIGN UP FAIL')

    else:
        return HttpResponse('POST REQUESTED')

def sign_up(data):

    username = data['uid']
    try:
        user = User.objects.create_user(username, settings.DEFAULT_EMAIL, settings.DEFAULT_PASSWORD)
        return True

    except IntegrityError:
        print('User ' + username + ' already exists')
        return False

#TODO 检查EXPIRY 确认是否使用refresh_token
def update_weibo_profile(data):

    uid = data['uid']
    access_token = data['access_token']
    refresh_token = data['refresh_token']
    expire_date = data['expire_date']

    response = urllib.request.urlopen(Weibo_API.USERS_SHOW)
    data = json.loads(response.read().decode('utf8'))

    user = User.objects.get(username = uid)
    profile = Weibo_Profile.objects.get(user = user)

    if profile is None:
        try:
            profile = Weibo_Profile(
                user = user,
                id = data['id'],
                idstr = data['idstr'],
                screen_name = data['screen_name'],
                name = data['name'],

                province = data['province'],
                city = data['city'],

                location = data['location'],
                description = data['description'],

                url = data['url'],
                profile_image_url = data['profile_image_url'],
                profile_url = data['profile_url'],

                domain = data['domain'],
                weihao = data['weihao'],
                gender = data['gender'],

                followers_count = data['followers_count'],
                friends_count = data['friends_count'],
                statuses_count = data['statuses_count'],
                favourites_count = data['favourites_count'],

                created_at = data['created_at'],

                following = data['following'],
                allow_all_act_msg = data['allow_all_act_msg'],
                geo_enabled = data['geo_enabled'],
                verified = data['verified'],

                verified_type = data['verified_type'],

                remark = data['remark'],

                allow_all_comment = data['allow_all_comment'],

                avatar_large = data['avatar_large'],
                avatar_hd = data['avatar_hd'],
                verified_reason = data['verified_reason'],

                follow_me = data['follow_me'],

                online_status = data['online_status'],
                bi_followers_count = data['bi_followers_count'],
                lang = data['lang'],
            )

            profile.save()
            return True
        except:
            print('DB Error')
            return False

    else:
        try:
            profile.id = data['id']
            profile.idstr = data['idstr']
            profile.screen_name = data['screen_name']
            profile.name = data['name']

            profile.province = data['province']
            profile.city = data['city']

            profile.location = data['location']
            profile.description = data['description']

            profile.url = data['url']
            profile.profile_image_url = data['profile_image_url']
            profile.profile_url = data['profile_url']

            profile.domain = data['domain']
            profile.weihao = data['weihao']
            profile.gender = data['gender']

            profile.followers_count = data['followers_count']
            profile.friends_count = data['friends_count']
            profile.statuses_count = data['statuses_count']
            profile.favourites_count = data['favourites_count']

            profile.created_at = data['created_at']

            profile.following = data['following']
            profile.allow_all_act_msg = data['allow_all_act_msg']
            profile.geo_enabled = data['geo_enabled']
            profile.verified = data['verified']

            profile.verified_type = data['verified_type']

            profile.remark = data['remark']

            profile.allow_all_comment = data['allow_all_comment']

            profile.avatar_large = data['avatar_large']
            profile.avatar_hd = data['avatar_hd']
            profile.verified_reason = data['verified_reason']

            profile.follow_me = data['follow_me']

            profile.online_status = data['online_status']
            profile.bi_followers_count = data['bi_followers_count']
            profile.lang = data['lang']

            profile.save()
            return True
        except:
            print('DB Error')
            return False



@login_required
def change_password(request):
    if request.method == 'POST':

        data = json.loads(request.read().decode('utf8'))

        try:
            user = User.objects.get(username=data['username'])
            user.set_password(data['password'])
            user.save()
            return HttpResponse('PASSWORD CHANGED')

        except:
            return HttpResponse('DB Error')


    else:
        return HttpResponse('POST REQUESTED')



@login_required
def change_email(request):
    if request.method == 'POST':

        data = json.loads(request.read().decode('utf8'))

        try:
            user = User.objects.get(username=data['username'])
            user.email = data['email']
            user.save()
            return HttpResponse('EMAIL CHANGED')

        except:
            return HttpResponse('DB Error')


    else:
        return HttpResponse('POST REQUESTED')


def log_out(request):
    if request.method == 'POST':
        logout(request)
        return HttpResponse('LOGOUT FINE')
    else:
        logout(request)
        return HttpResponse('LOGOUT FINE')


# def upload_avatar(request):
#     if request.method == 'POST':
#         form = ImageForm(request.POST, request.FILES)
#         if form.is_valid():
#             avatar = Avatar(imgfile=request.FILES['imgfile'])
#             avatar.save()
#
#             # Redirect to the document list after POST
#             return HttpResponseRedirect(reverse('myapp.views.list'))
#     else:
#         form = ImageForm()
#
#     # Load documents for the list page
#     documents = Document.objects.all()
#
#     # Render list page with the documents and the form
#     return render_to_response(
#         'myapp/list.html',
#         {'documents': documents, 'form': form},
#         context_instance=RequestContext(request)
#     )


@login_required
def download_avatar(request):
    avatar = open(settings.STATIC_URL + 'avatar/test.jpg', 'rb').read()
    return HttpResponse(avatar, content_type='image/jpeg')


