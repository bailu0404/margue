@echo off

cls
del /F /Q bin\AudioServ.jar

javac -d classes src/com/joson/serv/*.java src/com/joson/serv/db/*.java src/com/joson/serv/sdk/*.java src/com/joson/serv/handler/*.java src/com/joson/lib/audio/codec/g711/*.java src/com/joson/lib/comm/*.java src/com/joson/lib/crypto/base64/*.java src/com/joson/lib/db/*.java src/com/joson/lib/proto/http/*.java src/org/json/*.java src/org/json/zip/*.java src/ch/unifr/nio/framework/*.java src/ch/unifr/nio/framework/ssl/*.java src/ch/unifr/nio/framework/transform/*.java
copy /B /Y src\ch\unifr\nio\framework\Strings* classes\ch\unifr\nio\framework\
jar cmf Manifest bin/AudioServ.jar -C classes com -C classes org -C classes ch -C . res
