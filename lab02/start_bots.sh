(./bot.sh seller | java -jar ./target/appClient.jar > /dev/null) &
sleep 2
(./bot.sh seller2 | java -jar ./target/appClient.jar > /dev/null) &
sleep 1
(./bot.sh seller3 | java -jar ./target/appClient.jar > /dev/null) &
sleep 1
(./bot.sh seller4 | java -jar ./target/appClient.jar > /dev/null) &
sleep 2
(./bot.sh seller5 | java -jar ./target/appClient.jar > /dev/null) &
