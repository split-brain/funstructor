# Commands

All commands are maps with two fields:
- type - string that represents type of command
- data - map that represents payload of command

Description is written using following pattern:
- **"string that represents name of command"** - this is placed in **type** field of command map
  - *members of command payload* - keys of map that is placed in **data** field of command map

## Client side commands
- **"game-request"** - is sent when user wants to play game
  - *user-name* - name of user that requested game
- **"start-game-ok"** - is sent when message **"start-game"** is received from server
  - *game-id* - uuid of game that user belongs to
- **"end-turn"** - is sent when player presses "End Turn" button
  - *game-id* - uuid of game that user belongs to
- **"action"** - is sent when player makes some action in his turn
  - *game-id* - uuid of game that user belongs to
  - *card-idx* - index of card that was used
  - *target* - "self", "opponent" or "both": indicates on whom card was used
  - *value* - optional field that can carry id or num
  - *funstruct-idx* (optional) - index of funstruct item on what card was used
- **"chat-message"**
  - *game-id* - uuid of game that user belongs to
  - *message* - string that represents message that was sent

## Server side commands
- **"game-request-ok"** - is sent as response to client's **"game-request"**
  - *uuid* - uuid that is assigned to player
  - *pending* - list of all pending players
- **"game-update"** - is sent when client-side state needs to be updated
  - *cards* - array of structures that allow client to render current player's cards
  - *current-turn* - uuid of player that who is allowed to make turn
  - *enemy-cards-num* - number of cards that enemy has
  - *funstruct* - funstruct of player
  - *enemy-funstruct* - funstruct of enemy
  - *messages* - array of log messages that server produced
- **"chat-message-response"**
  - *player-id* - id of player that sent message
  - *message* - message that was sent

## Examples
- **"game-request"** :
``` {:type "game-request"
     :data {:user-name "Oleh"}}```
