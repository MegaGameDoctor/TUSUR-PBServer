# TUSUR-PBServer

## Описание

Сервер разработан на языке Java с использованием библиотеки Netty (ACLFCloud) для организации соединения с устройствами
пользователей. Для хранения данных о пользователях и о полотне используется подключение к MySQL серверу.

## Запуск

Чтобы запустить PBServer - достаточно установить Java 17, скопировать файл PBServer.jar в удобную дирректорию и в этой
дирректории выполнить команду java -jar PBServer.jar. После этого программа закроется, выдав ошибку, а в этой же
дирректории появится файл config.pb, в котором нужно будет настроить подключение к базе данных MySQL. Таблицы и
начальные данные будут заполнены автоматически, это может занять время.
