//Google Scripts

function checkGmail() {
  var incidentBody = '';
  var incidentCount = 1;
  var threads = GmailApp.search('is:starred label:sms')

  for (var i = 0; i < threads.length; i++) {
    var messages = threads[0].getMessages()

    for (var j = 0; j < messages.length; j++) {
      var body = messages[j].getPlainBody();

      var index = body.indexOf('--');
      if (index > 0) {
        incidentBody += 'Incident' + incidentCount + ': ' + body.substring(0, index);
      } else {
        incidentBody += 'Incident' + incidentCount + ': ' + body + '\n';
      }
      incidentCount++;
      messages[j].unstar();
    }
  }

  if (incidentCount > 1) {
    GmailApp.sendEmail('trigger@recipe.ifttt.com', 'Home Security Incident', incidentBody);
    Logger.log(incidentBody);
  } else {
    Logger.log('All is right');
  }
}
