**Тестовое задание для собеседования**
***
Разработать Андроид приложение. Приложение состоит из 2х экранов:  

* Splash screen с произвольным логотипом  
* Map screen - карта mapbox, отображает текущее положение пользователя и маркеры в радиусе 10км от местоположения пользователя.  
Координаты маркеров для отображения получаются через API запрос к серверу с передачей координат пользователя.  
  
В качестве API использовать заглушку на свое усмотрение. После смены API адреса в приложении на рабочее API приложение не должно требовать каких-либо доработок.
Результат выложить на github или bitbucket.  

***
В качестве API для получения меток использовано Google Places API

***
Перед сборкой проекта пропишите в local.properties:  
MAPBOX_DOWNLOADS_TOKEN = Ваш ключ Mapbox
PLACES_API_KEY = Ваш ключ Google Places API