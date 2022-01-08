
local playerId = Context.getPlayer()
local x, y, z = Mobile.getMobilePosition(playerId)

Mobile.setMobileAnimation(playerId , "AnimEmote-Presenter")
-- Mobile.setMobileVisible(Context.getPlayer(), false)

Camera.setZoomFactor(2.5)
-- Camera.setUserZoomLocked(true)

-- UI.setUIVisible(false)
UI.loadTutorialDialog("bossSmasher","event.boss.smasher.13.title","event.boss.smasher.13.description")
-- UI.displayBackground(53)

-- display dialogs
-- invoke(0, 1, "UI.displaySplashText","quest.astrub.nation.choix")
invoke(0, 1, "UI.loadTutorialDialog", "beta.shushu", "beta.title", "beta.09.11.2021")

BubbleText.showText(playerId, "testing", 0, 40, false)

Sound.playLocalSound(11, 420800522001, 40, 2, -1, 0)

-- adds a compass location
UI.addCompass("compass", x, y + 1, z)

-- spawns a puddly
Actor.createActor(Mobile.generateClientMobileId(), 122601633, x, y + 1, z, 4)
