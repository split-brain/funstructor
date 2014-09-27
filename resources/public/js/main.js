var BASE_URL = location.host;
var PORT     = location.port;

var funs = funs || {};

(function(funs){
    var ws;

    funs.websocket = {
        _ws : new WebSocket("ws://" + BASE_URL + '/ws'),
        _events : {},
        send : function(m){
            this._ws.send(m);
            return this;
        },
        on : function(event, callback){
            this._events[event] = this._events[event] || [];
            this._events[event].push(callback);
            return this;
        },
        off : function(event, callback){
            if(this._events[event]){
                if(callback){
                    this._events[event] = this._events[event].filter(function(c){
                        return callback !== c;
                    });
                }else{
                    delete this._events[event];
                }
            }
            return this;
        }
    };
    
    ws = funs.websocket._ws;
    
    ws.onopen = function(event){
        ws.onmessage = function (event) {
            console.log(event.data);
        };
        funs.websocket.send(JSON.stringify({
            type : 'game-request'
        }));
    };
    
})(funs);