package one.mixin.bot

import java.io.IOException

class ServerErrorException(val code: Int) : IOException()