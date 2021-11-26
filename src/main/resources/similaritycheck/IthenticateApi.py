import iThenticate
import tornado.ioloop
import tornado.web
from tornado.escape import json_decode, json_encode, utf8
import json
import requests
import os
def initAndLogin(username,password):
    global client
    client=iThenticate.API.Client(username, password)
    return client.login()
class LoginHandler(tornado.web.RequestHandler):
    def post(self):
        data=json.loads(self.request.body)
        username=data["username"]
        password=data["password"]
        loginResult=initAndLogin(username,password)
        self.write(json_encode(loginResult))

class UploadHandler(tornado.web.RequestHandler):
    def post(self):
        data=json.loads(self.request.body)
        # username = data["username"]
        # password = data["password"]
        # client=initAndLogin(username,password)
        path=data["path"]
        folderId=data["folderId"]
        authorFirstName=data["authorFirstName"]
        authorLastName=data["authorLastName"]
        documentTitle=data["documentTitle"]
        result=client.documents.add(path, folderId, authorFirstName, authorLastName, documentTitle)
        print(result)
        jsonData = json.loads(json_encode(result.get('data')).strip("[").strip("]"))
        uploaded = json.loads(json_encode(jsonData.get('uploaded')).strip("[").strip("]"))
        self.write(json_encode(uploaded['id']))
class GetStatusHandler(tornado.web.RequestHandler):
    def post(self):
        data = json.loads(self.request.body)
        id=data["documentId"]
        # username = data["username"]
        # password = data["password"]
        # client = initAndLogin(username,password)
        result=client.documents.get(id)
        self.write(json_encode(result))
class GetAllDocumentsHandler(tornado.web.RequestHandler):
     def post(self):
        data=json.loads(self.request.body)
        id = data["folderId"]
        # username = data["username"]
        # password = data["password"]
        # client=initAndLogin(username,password)
        result=client.documents.all(id)
        self.write(json_encode(result))
class GetAllFoldersHandler(tornado.web.RequestHandler):
     def post(self):
        result=client.folders.all();
        self.write(json_encode(result))
class GetReportHandler(tornado.web.RequestHandler):
     def post(self):
         data=json.loads(self.request.body)
         id = data["documentId"]
         result = client.documents.get(id)
         jsonData = json.loads(json_encode(result.get('data')).strip("[").strip("]"))
         documents = json.loads(json_encode(jsonData.get('documents')).strip("[").strip("]"))
         if(documents['is_pending']==0):
           hasParts = False;
           for key in documents.keys():
               if (key == 'parts'):
                   hasParts = True
                   break
           if (hasParts):
               id = json.loads(json_encode(documents['parts']).strip("[").strip("]"))
               id=id['id']
               result = client.reports.get(id)
               jsonData = json.loads(json_encode(result.get('data')).strip("[").strip("]"))
               viewonlyurl=jsonData['view_only_url']
               url=jsonData['report_url']
               result={
                 "view_only_url":viewonlyurl,
                 "report_url":url
               }
               self.write(json_encode(result))
           else:
               self.write("false")
         else:self.write("false")

class ReportServerRouter(tornado.web.RequestHandler):
    def post(self):

        data = json.loads(self.request.body)
        print(data)
        if(data['service']!="report"):
            self.write("false")
        else:
            if(data['api']=="login"):
                jsonData =json.loads(data['data'])
                username=jsonData['username']
                password=jsonData['password']
                result=initAndLogin(username, password)
                self.write(json_encode(result))
            elif(data['api']=="upload"):
                jsonData=json.loads(data['data'])
                link = jsonData["link"]
                fileName=jsonData["fileName"]
                f=requests.get(link)
                with open(fileName,"wb") as code:
                    code.write(f.content)
                folderId =jsonData["folderId"]
                authorFirstName = jsonData["authorFirstName"]
                authorLastName = jsonData["authorLastName"]
                documentTitle = jsonData["documentTitle"]
                result = client.documents.add(fileName, folderId, authorFirstName, authorLastName, documentTitle)
                print(result)
                jsonData = json.loads(json_encode(result.get('data')).strip("[").strip("]"))
                uploaded = json.loads(json_encode(jsonData.get('uploaded')).strip("[").strip("]"))
                os.remove(fileName)
                self.write(json_encode(uploaded['id']))
            elif(data['api']=="getReport"):
                jsonData=json.loads(data['data'])
                id = jsonData["documentId"]
                result = client.documents.get(id)
                jsonData = json.loads(json_encode(result.get('data')).strip("[").strip("]"))
                documents = json.loads(json_encode(jsonData.get('documents')).strip("[").strip("]"))
                if (documents['is_pending'] == 0):
                    hasParts = False;
                    for key in documents.keys():
                        if (key == 'parts'):
                            hasParts = True
                            break
                    if (hasParts):
                        id = json.loads(json_encode(documents['parts']).strip("[").strip("]"))
                        id = id['id']
                        result = client.reports.get(id)
                        jsonData = json.loads(json_encode(result.get('data')).strip("[").strip("]"))
                        viewonlyurl = jsonData['view_only_url']
                        url = jsonData['report_url']
                        result = {
                            "view_only_url": viewonlyurl,
                            "report_url": url
                        }
                        self.write(json_encode(result))
                    else:
                        self.write("false")
                else:
                    self.write("false")

application = tornado.web.Application([
    (r"/login", LoginHandler),(r"/upload",UploadHandler),(r"/getStatusByDocumentId",GetStatusHandler),
    (r"/getDoucumentsByFolderId",GetAllDocumentsHandler),(r"/getAllFolders",GetAllFoldersHandler),
    (r"/getreports",GetReportHandler),(r"/reportServerRouter",ReportServerRouter)

])

if __name__ == "__main__":
    application.listen(8003)
    tornado.ioloop.IOLoop.instance().start()