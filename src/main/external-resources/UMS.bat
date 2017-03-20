@echo off
echo Universal Media Server
echo ---------------------
echo In case of troubles with UMS.exe, this shell will launch UMS in a more old fashioned way
echo You can try to reduce the Xmx parameter value if you keep getting "Cannot create Java virtual machine" errors...
echo Last word: You must have java installed ! http://www.java.com
echo ------------------------------------------------
pause
start javaw -Xmx768M -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8 -classpath update.jar;ums.jar net.pms.PMS
