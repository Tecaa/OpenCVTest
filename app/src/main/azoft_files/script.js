$(function() {
	var ie6 = !-[1,] && jQuery.browser.version < 7;

	var sub = null, speed = ie6 ? 10 : 250;
	$('#menu LI').hover(function() {
       	var s = $(this).find('.sub');
       	if (s.length > 0) {
       		sub = s.prev('.item').add(s).animate({opacity: 'show'}, speed);
       		var dls = s.find('.level'), w = 35, h = 0;
   	        dls.each(function() {
       	    	var dl = $(this), dh = dl.height();
       	    	w += dl.outerWidth();
       	    	h = h < dh ? dh : h;
           	});
           	dls.height(h);
			// alert(dls.eq(0).outerWidth()+' '+dls.length);
          	// if (dls.length > 2) {
           		// s.css('left', (-dls.eq(0).outerWidth()+25) + 'px');
           	// }
           	s.width(w).addClass('build');
       	}
	}, function() {
		sub && ie6 && sub.hide();
		sub && !ie6 && sub.animate({opacity: 'hide'}, speed);
	});


    $('#header .lang').click(function() {
		$(this).find('DL').show();
	});
	$('#header .lang').hover(function() {
	}, function() {
		$(this).find('DL').hide();
	});

	var subnav = $('#nav DL');
	$('#nav .tlevel').click(function() {
		var s = $(this).next('DL');
		if (s.length > 0) {
			subnav.not(s).slideUp(250).parent('LI').removeClass('active');
			s.slideDown(250).parent('LI').addClass('active');
			return !1;
		}
	});

	$('SELECT').each(function() {
		var s = $(this).hide(), id = s.attr('id'), timer = null;
		s.removeAttr('id');
		var i = $('<div class="ls-text ls-select"><input type="text" id="' + id +
			'" readonly="true" value="' + s.find(':selected').text() +
			'" /></div>').insertAfter(s);
        var pl = $('<div class="ls-select-list"/>').appendTo('body').hide().hover(function() {
           	clearTimeout(timer);
       	}, function() {
      		timer = setTimeout(function() {
         		pl.hide();
  	       	}, 300);
      	});
        var li = '';
		var option = s.find('OPTION').each(function() {
			var o = $(this);
			li += '<li><a href="#"' + (o.attr('selected') ? ' class="active"' : '') + '>' + o.text() + '</a></li>';
		});
		pl.append('<ul>' + li + '</ul>').find('A').click(function() {
        	pl.find('A').removeClass('active');
            var a = $(this).addClass('active');
            i.find('INPUT').val(a.text());
            option.eq(a.parent().index()).attr('selected', true);
            s.triggerHandler('change');
            pl.hide();
            return!1;
		});
        $('<a href="#"></a>').appendTo(i).click(function() {
            if (pl.css('display') == 'none') {
              	var o = i.offset();
              	pl.css({ 'top': o.top + i.height()+2, 'left': o.left }).width(i.width()+4).show();
        	}
            else pl.hide();
            return!1;
		}).mouseout(function() {
			timer = setTimeout(function() {
				pl.hide();
			}, 100);
		});
        i.find('INPUT').hover(function() {
           	clearTimeout(timer);
        }, function() {
           	timer = setTimeout(function() {
        		pl.hide();
        	}, 300);
		});
	});
    /*
	$('.tabs A').click(function() {
		var A = $(this), li = A.parent();
		if (!li.hasClass('active')) {
			$(li.parent().find('LI.active').removeClass('active').find('A').attr('rel'))
				.animate({opacity: 'hide'}, 250, function() {
					$(A.attr('rel')).animate({opacity: 'show'}, 250);
					li.addClass('active');
				});
		}
		return!1;
	});
	*/


	ie6 && $('.ls-btn').hover(function() {
		$(this).addClass('hover');
	}, function() {
		$(this).removeClass('hover');
	});

	$('.techs A').hover(function() {
		$(this).find('IMG').stop().fadeTo(250, 1);
	}, function() {
		$(this).find('IMG').stop().fadeTo(250, 0);
	});

});




/* Request a Free Quote */
function testSYRData1() {
	if (document.submit_your_request.elements['name'].value == "") {
		alert ("\Please, enter \"Name\"!\t\n")
		document.submit_your_request.elements['name'].focus();
		return false;
	}
	if (document.submit_your_request.elements['company'].value == "") {
		alert ("\Please, enter \"Company\"!\t\n")
		document.submit_your_request.elements['company'].focus();
		return false;
	}
	if (document.submit_your_request.elements['email'].value == "") {
		alert ("\Please, enter \"E-mail\"!\t\n")
		document.submit_your_request.elements['email'].focus();
		return false;
	}
	if (document.submit_your_request.elements['phone'].value == "") {
		alert ("\Please, enter \"Phone\"!\t\n")
		document.submit_your_request.elements['phone'].focus();
		return false;
	}
	return presubmitContactForm();
}
function presubmitContactForm() {
	var oCaptcha = document.getElementById('captcha');
	if (oCaptcha != undefined)
	{
		sCaptcha = oCaptcha.value;
		bValidateCaptcha = false;

		$.ajax({
			  type: 'POST',
			  async: false,
			  url: '/validate_captcha.php',
			  data: { 'captcha': $('#captcha').val()}, 
			  success: function(data) {
				  bValidateCaptcha = data;
			  }
		});
		
		if (bValidateCaptcha) {
			return true;
		} else {
			$('#captcha_image').attr('src', $('#captcha_image').attr('src') + "?" + new Date().getTime());
			alert('WRONG captcha!');
		}
	}
	return false;
}
var getSmallUrlToServer = function(longUrl, links, text, linksTwo) {
    var key = "R_6f92a19087854101b8029e0d0509bb4c";
    var username = "valeryan15";
    $.ajax({
        url:"http://api.bit.ly/v3/shorten",
        data:{longUrl:longUrl,apiKey:key,login:username},
        dataType:"jsonp",
        success: function(result) {
            text+= 'url=' + result.data.url;
            $(links).attr('href', text);
            $(linksTwo).attr('href', text);
        }
    })
}
var replaceUrl = function() {
    var links = $('.tm-ctt-text').children('a');
    var linksTwo = $('.tm-ctt-text').parent().children('p').children('a');
    var l = links.length;
    if(l > 0){
        for (var i = 0; i < l; i++) {
            var href = $(links[i]).attr('href');
            var cache = href.split('url=');
            var longUrl = cache[1];
            var smallUrl = cache[0];
            getSmallUrlToServer(longUrl,links[i], smallUrl, linksTwo[i]);
        }
    }
}
$(document).ready(function(){
	$('.input-box .input-text').parent().removeClass("focus");
	$('.input-box .input-text').click(function() {
		$('.input-box').removeClass("focus");
		$(this).parent().addClass("focus");
	});
    replaceUrl()
	$(document).bind('click', function(e) {
		var $clicked = $(e.target);
		if (! $clicked.parents().hasClass("input-box"))
			$('.input-box').removeClass("focus");
	});
});