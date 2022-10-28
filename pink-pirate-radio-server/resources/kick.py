
for pin in BUTTONS:
    GPIO.add_event_detect(pin, GPIO.FALLING, handle_button, bouncetime=100)

run_user_event_handler('on_program_start')

while True:
    time.sleep(1)
