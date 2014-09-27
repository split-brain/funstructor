'use strict';

var BASE_URL = location.host;
var PORT     = location.port;

var funs = funs || {};

// GAME STATE
(function(funs){
    funs.state = {
        
    };
})(funs);

// VIEWS SWITCHER
(function(funs){
    funs.switch = function(view, options){
        React.renderComponent(view(options), funs.$main[0]);
    };
})(funs);

// LOADER
(function(funs){
    var $loading = $('<div id="loading"><span class="loader"><span class="loader-inner"></span></span></div>');
    funs.startLoading = function(){
        $loading.appendTo(funs.$body);
        $loading.show();
    };
    funs.stopLoading = function(){
        $loading.hide();
    };
})(funs);

// WEB SOCKET
(function(funs){
    var ws;

    funs.websocket = {
        _ws : new WebSocket("ws://" + BASE_URL + '/ws'),
        _events : {},
        send : function(m){
            if(typeof m !== 'string'){
                m = JSON.stringify(m);
            }
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
            var message = JSON.parse(event.data),
                events  = funs.websocket._events[message.type];
            if(events){
                events.forEach(function(callback){
                    callback(message.data);
                });
            }
        };
    };
    
})(funs);

// WEBSOCKET ROUTER
(function(funs){
    var ws = funs.websocket;
    
    var ROUTES = {
        'game-request-ok' :  function(data){
            console.log('game-request-ok', data);
            funs.startLoading();
            funs.state.uuid = data.uuid;
        },
        'start-game' : function(data){
            console.log('start-game', data);
            funs.state['game-id'] = data['game-id'];
            funs.state['task']    = data['task'] || 'no task yet';
            ws.send({
                type: 'start-game-ok',
                data: {
                    'game-id' : data['game-id']
                }
            });
        },
        'game-update' : function(data){
            funs.stopLoading();
            funs.state.gameData = data;
            funs.switch(funs.Views.GameTable, data);
            setTimeout(function(){
                var $cards = $('.your.hand > .card');
                var $func = $('.your.hand > .card');
                $cards.attr('ondragstart', 'funs.ondragstart(event)');
            }, 100);
        }
    };

    for(var route in ROUTES){
        ws.on(route, ROUTES[route]);
    }
})(funs);


// START
(function(funs){
    funs.$body = $('body');
    funs.$main = $('main').eq(0);

    // draw cards on a field
    var flyingCards = document.getElementById('flyingCards');
    var CARDS = [
        "action_discard_1.svg"     
        ,"action_equality_1.svg"    
        ,"action_equality_2.svg"    
        ,"action_equality_3.svg"    
        ,"action_thief_1.svg"       
        ,"duration_luck_1.svg"      
        ,"duration_luck_2.svg"      
        ,"mutator_left_gap.svg"     
        ,"mutator_pos_gap.svg"      
        ,"mutator_right_gap.svg"    
        ,"mutator_shot.svg"         
        ,"terminal_id.svg"          
        ,"terminal_left_paren.svg"  
        ,"terminal_left_square.svg" 
        ,"terminal_num.svg"         
        ,"terminal_right_paren.svg" 
        ,"terminal_right_square.svg"
        ,"action_discard_1.svg"     
    ];
    var requested = false;
    
    drawCards();
    
    // MOCK
//    funs.switch(funs.Views.GameTable, JSON.parse('{"type":"game-update","data":{"enemy-funstruct":[{"terminal":"gap","value":null}],"enemy-cards-num":6,"current-turn":"7902e56e-60aa-49f8-b589-471ae6d0b866","ready":true,"opponent":"7902e56e-60aa-49f8-b589-471ae6d0b866","cards":[{"description":"fill the gap on your funstruct with left square","name":"LEFT SQUARE","value":"left-square","type":"terminal","weight":100,"target":"self","img":"terminal_left_square.svg"},{"description":"fill the gap on your funstruct with right square","name":"RIGHT SQUARE","value":"right-square","type":"terminal","weight":100,"target":"self","img":"terminal_right_square.svg"},{"description":"fill the gap on your funstruct with left paren","name":"Left Paren","value":"left-paren","type":"terminal","weight":100,"target":"self","img":"terminal_left_paren.svg"},{"description":"fill the gap on your funstruct with right square","name":"RIGHT SQUARE","value":"right-square","type":"terminal","weight":100,"target":"self","img":"terminal_right_square.svg"},{"description":"fill the gap on your funstruct with integer number","name":"Number","value":"num","type":"terminal","weight":50,"target":"self","img":"terminal_num.svg"},{"description":"fill the gap on your funstruct with identifier, according to clojure regexp","name":"ID","value":"id","type":"terminal","weight":100,"target":"self","img":"terminal_id.svg"}],"funstruct":[{"terminal":"gap","value":null}]}}').data);
    
    $(function(){
        var $form = $('#startForm');
        var $input = $form.find('.input > input').eq(0);
        $input.focus();
        $form.on('submit', function(e){
            e.preventDefault();
            if(requested){
                return;
            }
            var name = $input.val();
    //        requested = true;
            funs.websocket.send({
                type: "game-request",
                data: {
                    'user-name' : name
                }
            });
        });
    });
    
    function drawCards(){
        var width  = funs.$body.width();
        var height = funs.$body.height();
        var cards = React.createClass({
            render: function() {
                var R = React.DOM;
                var d = 75;
                var cards = this.props.cards;
                
                return R.div({
                    key: 'cards'
                }, cards.map(function(card, i){
                    var xr = Math.round(Math.random() * d);
                    var yr = Math.round(Math.random() * d);
                    var zr = Math.round(Math.random() * d);
                    var transform = 'rotateX( ' + xr + 'deg ) rotateY( ' + yr + 'deg ) rotateZ( ' + zr + 'deg ) ';
                    
                    var style = {
                        '-moz-transform': transform,
                        '-webkit-transform': transform,
                        '-o-transform': transform,
                        '-ms-transform': transform,
                        transform: transform,
                        left : (200 + Math.random() * (width - 400)) + 'px',
                        top  : (200 + Math.random() * (height - 400)) + 'px'
                    };
                    return funs.Views.Card({
                        url: card,
                        style: style
                    });
                }));
            }
        });
        
        React.renderComponent(cards({
            cards : CARDS
        }), flyingCards);
    }
})(funs);

// DRAG_N_DROP
(function(funs){
    funs.ondragstart = function(e){
        console.log('ondragstart', e);
    };
    funs.ondrag = function(e){
        console.log('ondrag', e);
    };
    funs.ondragover = function(e){
        console.log('ondragover', e);
    };
    funs.ondrop = function(e){
        console.log('ondrop', e);
    };
})(funs);

(function(funs){
})(funs);
