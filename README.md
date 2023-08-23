# volk's steam-library-export

Ever wanted to download the full list of games you have in your steam library?  
Or to know how many games you have actually launched?  
Or even to get some game suggestions?

If you do - here you go, a script that makes an entire xlsx file that contains all of these.

## Usage:

- install sbt
- `sbt "run -usr <your profile id> -api <api key> -f <result file path>"`

## FYI:

- You can use your custom profile id (meaning one that you set up yourself, like `epicgamer`, not one that steam has
  assigned to you, like `12345678901234567`)
- You can get your api key [here](https://steamcommunity.com/dev/apikey)

## Plans:

- better looking spreadsheets (trying to figure out how styling works)
