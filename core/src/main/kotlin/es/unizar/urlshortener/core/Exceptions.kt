package es.unizar.urlshortener.core

class InvalidUrlException(url: String) : Exception("[$url] does not follow a supported schema")

class RedirectionNotFound(key: String) : Exception("[$key] is not known")

class InformationNotFound(key: String) : Exception("There is no information for the hash value: [$key]")

class CalculandoException(key: String) : Exception("[$key]")

class InvalidExist(key: String) : Exception("[$key]")
