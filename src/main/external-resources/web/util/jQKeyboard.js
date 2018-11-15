/*
 * poiyee.ho
 */
(function($){
    var keyboardLayout = {
        'layout': [
            // alphanumeric keyboard type
            // text displayed on keyboard button, keyboard value, keycode, column span, new row
            [
                [
					['a', 'a', 65, 0, true ], ['b', 'b', 66, 0, false], ['c', 'c', 67, 0, false], ['d', 'd', 68, 0, false], ['e', 'e', 69, 0, false], ['f', 'f', 70, 0, false], ['g', 'g', 71, 0, false], ['    ',   '8', 8, 0, false],
					['h', 'h', 72, 0, true ], ['i', 'i', 73, 0, false], ['j', 'j', 74, 0, false], ['k', 'k', 75, 0, false], ['l', 'l', 76, 0, false], ['m', 'm', 77, 0, false], ['n', 'n', 78, 0, false], ['&123','&123', 16, 2, false],
					['ñ', 'ñ',165, 0, true ], ['o', 'o', 79, 0, false], ['p', 'p', 80, 0, false], ['q', 'q', 81, 0, false], ['r', 'r', 82, 0, false], ['s', 's', 83, 0, false], ['t', 't', 84, 0, false], 
					['u', 'u', 85, 0, true ], ['v', 'v', 86, 0, false], ['w', 'w', 87, 0, false], ['x', 'x', 88, 0, false], ['y', 'y', 89, 0, false], ['z', 'z', 90, 0, false], ['`', '`',192, 0, false],
					['SPACE', '32', 32,4, true],['ERASE', '46', 46,4, false],['SEARCH', '13', 13,4, false]
				],
				[
					['1', '1', 49, 0, true], ['2', '2', 50, 0, false], ['3', '3', 51, 0, false],['&', '&', 55, 0, false],['#', '#', 51, 0, false],['(', '(', 57, 0, false], [')', ')', 48, 0, false], ['    ',   '8', 8, 0, false],
					['4', '4', 52, 0, true], ['5', '5', 53, 0, false], ['6', '6', 54, 0, false],['@', '@', 50, 0, false], ['!', '!', 49, 0, false], ['?', '?', 191, 0, false],[':', ':', 186, 0, false], ['ABC','ABC', 16, 2, false],
					['7', '7', 55, 0, true], ['8', '8', 56, 0, false], ['9', '9', 57, 0, false], ['0', '0', 48, 0, false],['.', '.', 190, 0, false],['_', '_', 189, 0, false],['"', '"', 222, 0, false],
					['SPACE', '32', 32,4, true],['ERASE', '46', 46,4, false],['SEARCH', '13', 13,4, false]
				
					
                    /*['<i class="fa fa-sign-out-alt fa-rotate-270">', '16', 16, 3, true], ['1', '1', 49, 0, false], ['2', '2', 50, 0, false], ['3', '3', 51, 0, false], ['4', '4', 52, 0, false], ['5', '5', 53, 0, false], ['6', '6', 54, 0, false],['7', '7', 55, 0, false], ['8', '8', 56, 0, false], ['9', '9', 57, 0, false], ['0', '0', 48, 0, false], ['', '8', 8, 3, false],['Clear All', '46', 46, 3, false],
                    ['#@!', '#@!', 0, 3, true],['q', 'q', 81, 0, false], ['w', 'w', 87, 0, false], ['e', 'e', 69, 0, false], ['r', 'r', 82, 0, false], ['t', 't', 84, 0, false], ['y', 'y', 89, 0, false], ['u', 'u', 85, 0, false], ['i', 'i', 73, 0, false], ['o', 'o', 79, 0, false], ['p', 'p', 80, 0, false], ['^', '^', 54, 0, false],['*', '*', 56, 0, false],['Enter', '13', 13, 3, false],
					['a', 'a', 65, 0, true], ['s', 's', 83, 0, false], ['d', 'd', 68, 0, false], ['f', 'f', 70, 0, false], ['g', 'g', 71, 0, false], ['h', 'h', 72, 0, false], ['j', 'j', 74, 0, false], ['k', 'k', 75, 0, false], ['l', 'l', 76, 0, false],['~', '~', 192, 0, false], ['@', '@', 50, 0, false], ['!', '!', 49, 0, false], 
                    ['z', 'z', 90, 0, true], ['x', 'x', 88, 0, false], ['c', 'c', 67, 0, false], ['v', 'v', 86, 0, false], ['b', 'b', 66, 0, false], ['n', 'n', 78, 0, false],['m', 'm', 77, 0, false], [',', ',', 188, 0, false], ['.', '.', 190, 0, false],['?', '?', 191, 0, false], ['-', '-', 189, 0, false], 
					['<i class="fa fa-cog">', 'settings', 4, 2, true], ['Space', '32', 32, 12, false],['www.', 'www.', 1, 0, false],['.com', '.com', 1, 0, false],['/', '/', 191, 0, false],['<-', '<-', 2, 0, false],['v', 'v', 2, 0, false],['->', '->', 3, 0, false],['Cancel', '27', 27, 3, false]
                ],
                [
                    ['<i class="fa fa-sign-out-alt fa-rotate-270">', '16', 16, 3, true], ['!', '!', 49, 0, false], ['@', '@', 50, 0, false], ['#', '#', 51, 0, false], ['$', '$', 52, 0, false], ['%', '%', 53, 0, false], ['^', '^', 54, 0, false], 
                    ['&', '&', 55, 0, false], ['*', '*', 56, 0, false], ['(', '(', 57, 0, false], [')', ')', 48, 0, false], ['<i class="fa fa-tag" data-fa-transform="rotate--45 right-10"></i><i class="fa fa-caret-left" data-fa-transform="left-7"></i><i class="fa fa-times" style="color: #bcbfc3;text-shadow: 1px 1px 1px #ccc;" data-fa-transform="shrink-6 left-10"></i>', '8', 8, 3, true],['Clear All', '46', 46, 3, false],
                    ['Q', 'Q', 81, 0, true], ['W', 'W', 87, 0, false], ['E', 'E', 69, 0, false], ['R', 'R', 82, 0, false], ['T', 'T', 84, 0, false], ['Y', 'Y', 89, 0, false], ['U', 'U', 85, 0, false], 
                    ['I', 'I', 73, 0, false], ['O', 'O', 79, 0, false], ['P', 'P', 80, 0, false], ['{', '{', 219, 0, false], ['}', '}', 221, 0, false], ['|', '|', 220, 0, false],
                    ['A', 'A', 65, 0, true], ['S', 'S', 83, 0, false], ['D', 'D', 68, 0, false], ['F', 'F', 70, 0, false], ['G', 'G', 71, 0, false], ['H', 'H', 72, 0, false], ['J', 'J', 74, 0, false], 
                    ['K', 'K', 75, 0, false], ['L', 'L', 76, 0, false], [':', ':', 186, 0, false], ['"', '"', 222, 0, false], ['Enter', '13', 13, 3, false],
                    ['~', '~', 192, 0, true], ['Z', 'Z', 90, 0, false], ['X', 'X', 88, 0, false], ['C', 'C', 67, 0, false], ['V', 'V', 86, 0, false], ['B', 'B', 66, 0, false], ['N', 'N', 78, 0, false], 
                    ['M', 'M', 77, 0, false], ['<', '<', 188, 0, false], ['>', '>', 190, 0, false], ['?', '?', 191, 0, false], ['<i class="fa fa-sign-out-alt fa-rotate-270">', '16', 16, 2, false],
                    ['+', '+', 187, 0, false],['`', '`', 192, 0, false], ['Space', '32', 32, 12, false], ['_', '_', 189, 0, false], ['Cancel', '27', 27, 3, false],[';', ';', 186, 0, false], ['&#39;', '\'', 222, 0, false], ['Enter', '13', 13, 3, false],['[', '[', 219, 0, false], [']', ']', 221, 0, false], ['&#92;', '\\', 220, 0, false] ,['=', '=', 187, 0, false]*/
                ]
            ]
        ]
    };
    var activeInput = {
        'htmlElem': '',
        'initValue': '',
        'keyboardLayout': keyboardLayout,
        'keyboardType': '0',
        'keyboardSet': 0,
        'dataType': 'string',
        'isMoney': false,
        'thousandsSep': ',',
        'disableKeyboardKey': false
    };

    /*
     * initialize keyboard
     * @param {type} settings
     */
    $.fn.initKeypad = function(settings){
        //$.extend(activeInput, settings);

        $(this).focus(function(e){
            activateKeypad(e.target);
        });
    };
    
    /*
     * create keyboard container and keyboard button
     * @param {DOM object} targetInput
     */
    function activateKeypad(targetInput){
        if($('div.jQKeyboardContainer').length === 0)
        {
            activeInput.htmlElem = $(targetInput);
            activeInput.initValue = $(targetInput).val();

            $(activeInput.htmlElem).addClass('focus');
            createKeypadContainer();
            createKeypad(0);
        }
    }
    
    /*
     * create keyboard container
     */
    function createKeypadContainer(){
        var container = document.createElement('div');
        container.setAttribute('class', 'jQKeyboardContainer footer');
        container.setAttribute('id', 'jQKeyboardContainer');
        container.setAttribute('name', 'keyboardContainer' + activeInput.keyboardType);
        
        $('.overlay').append(container);
    }
    
    /*
     * create keyboard
     * @param {Number} set
     */
    function createKeypad(set){
        $('#jQKeyboardContainer').empty();
        
        var layout = activeInput.keyboardLayout.layout[activeInput.keyboardType][set];

        for(var i = 0; i < layout.length; i++){

            if(layout[i][4]){
                var row = document.createElement('div');
                row.setAttribute('class', 'jQKeyboardRow row');
                row.setAttribute('name', 'jQKeyboardRow');
                $('#jQKeyboardContainer').append(row);
            }

            var button = document.createElement('button');
            button.setAttribute('type', 'button');
            button.setAttribute('name', 'key' + layout[i][2]);
            button.setAttribute('id', 'key' + layout[i][2]);
            button.setAttribute('class', 'jQKeyboardBtn' + ' ui-button-colspan-' + layout[i][3]);
            button.setAttribute('data-text', layout[i][0]);
            button.setAttribute('data-value', layout[i][1]);
            button.innerHTML = layout[i][0];
            
            $(button).click(function(e){				
				getKeyPressedValue(e.target); 
            });
			if(layout[i][2]==13 || layout[i][2]==32 || layout[i][2]==46){				
				var buttonContainer = document.createElement('div');
				buttonContainer.setAttribute('id', 'container-key' + layout[i][2]);
				buttonContainer.setAttribute('class', 'col-xs-4');
				$(buttonContainer).append(button);
				$(row).append(buttonContainer);
			}
			else
			{
				$(row).append(button);
			}
			
        }
		$('#jQKeyboardContainer').find('.jQKeyboardRow:last').addClass('special-keys-container');
		$("#key8").addClass("icon-key-delete");
    }
    /*
     * remove keyboard from kepad container
     */
    function removeKeypad(){
        $('#jQKeyboardContainer').remove();
        $(activeInput.htmlElem).removeClass('focus');
    }
    
    /*
     * handle key pressed
     * @param {DOM object} clickedBtn
     */
    function getKeyPressedValue(clickedBtn){
        var caretPos = getCaretPosition(activeInput.htmlElem);
        var keyCode = parseInt($(clickedBtn).attr('name').replace('key', ''));
        
        var currentValue = $(activeInput.htmlElem).val();
        var newVal = currentValue;
        var closeKeypad = false;
        
        /*
         * TODO
        if(activeInput.isMoney && activeInput.thousandsSep !== ''){
            stripMoney(currentValue, activeInput.thousandsSep);
        }
        */
        
        switch(keyCode){
            case 8:     // backspace key
                newVal = onDeleteKeyPressed(currentValue, caretPos);
		          caretPos--;
                break;
            case 13:    // enter key
                closeKeypad = onEnterKeyPressed();
                break;
            case 16:    // shift key
                onShiftKeyPressed();
                break;
            case 27:    // cancel key
                closeKeypad = true;
                newVal = onCancelKeyPressed(activeInput.initValue);
                break;
            case 32:    // space key
                newVal = onSpaceKeyPressed(currentValue, caretPos);
                caretPos++;
		break;
            case 46:    // clear key
                newVal = onClearKeyPressed();
                break;
            case 190:   // dot key
                newVal = onDotKeyPressed(currentValue, $(clickedBtn), caretPos);
                caretPos++;
                break;
            default:    // alpha or numeric key
                newVal = onAlphaNumericKeyPressed(currentValue, $(clickedBtn), caretPos);
                caretPos++;
                break;
        }
        
        // update new value and set caret position
        $(activeInput.htmlElem).val(newVal);
        setCaretPosition(activeInput.htmlElem, caretPos);

        if(closeKeypad){
            removeKeypad();
            $(activeInput.htmlElem).blur();
        }
    }
    
    /*
     * handle delete key pressed
     * @param value 
     * @param inputType
     */
    function onDeleteKeyPressed(value, caretPos){
        var result = value.split('');
        
        if(result.length > 1){
            result.splice((caretPos - 1), 1);
            return result.join('');
        }
    }
    
    /*
     * handle shift key pressed
     * update keyboard layout and shift key color according to current keyboard set
     */
    function onShiftKeyPressed(){
        var keyboardSet = activeInput.keyboardSet === 0 ? 1 : 0;
        activeInput.keyboardSet = keyboardSet;

        createKeypad(keyboardSet);
        
        if(keyboardSet === 1){
            //$('button[name="key16"').addClass('shift-active');
        }else{
            //$('button[name="key16"').removeClass('shift-active');
        }
    }
    
    /*
     * handle space key pressed
     * add a space to current value
     * @param {String} curVal
     * @returns {String}
     */
    function onSpaceKeyPressed(currentValue, caretPos){
        return insertValueToString(currentValue, ' ', caretPos);
    }
    
    /*
     * handle cancel key pressed
     * revert to original value and close key pad
     * @param {String} initValue
     * @returns {String}
     */
    function onCancelKeyPressed(initValue){
        return initValue;
    }
    
    /*
     * handle enter key pressed value
     * TODO: need to check min max value
     * @returns {Boolean}
     */
    function onEnterKeyPressed(){
		$('#SearchForm').submit();
		//$('.onFocus').click();
        return true;
    }
    
    /*
     * handle clear key pressed
     * clear text field value
     * @returns {String}
     */
    function onClearKeyPressed(){
        return '';
    }
    
    /*
     * handle dot key pressed
     * @param {String} currentVal
     * @param {DOM object} keyObj
     * @returns {String}
     */
    function onDotKeyPressed(currentValue, keyElement, caretPos){
        return insertValueToString(currentValue, keyElement.attr('data-value'), caretPos);
    }
    
    /*
     * handle all alpha numeric keys pressed
     * @param {String} currentVal
     * @param {DOM object} keyObj
     * @returns {String}
     */
    function onAlphaNumericKeyPressed(currentValue, keyElement, caretPos){
        return insertValueToString(currentValue, keyElement.attr('data-value'), caretPos);
    }
    
    /*
     * insert new value to a string at specified position
     * @param {String} currentValue
     * @param {String} newValue
     * @param {Number} pos
     * @returns {String}
     */
    function insertValueToString(currentValue, newValue, pos){
        var result = currentValue.split('');
        result.splice(pos, 0, newValue);
        
        return result.join('');
    }
    
   /*
    * get caret position
    * @param {DOM object} elem
    * @return {Number}
    */
    function getCaretPosition(elem){
        var input = $(elem).get(0);

        if('selectionStart' in input){    // Standard-compliant browsers
            return input.selectionStart;
        } else if(document.selection){    // IE
            input.focus();
            
            var sel = document.selection.createRange();
            var selLen = document.selection.createRange().text.length;
            
            sel.moveStart('character', -input.value.length);
            return sel.text.length - selLen;
        }
    }
    
    /*
     * set caret position
     * @param {DOM object} elem
     * @param {Number} pos
     */
    function setCaretPosition(elem, pos){
        var input = $(elem).get(0);
        
        if(input !== null) {
            if(input.createTextRange){
                var range = elem.createTextRange();
                range.move('character', pos);
                range.select();
            }else{
                input.focus();
                input.setSelectionRange(pos, pos);
            }
        }
    }
})(jQuery);
