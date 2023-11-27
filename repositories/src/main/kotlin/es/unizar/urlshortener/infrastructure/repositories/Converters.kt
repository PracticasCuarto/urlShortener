@file:Suppress("WildcardImport")

package es.unizar.urlshortener.infrastructure.repositories

import es.unizar.urlshortener.core.*

/**
 * Extension method to convert a [ClickEntity] into a domain [Click].
 */
fun ClickEntity.toDomain() = Click(
    hash = hash,
    created = created,
    properties = ClickProperties(
        ip = ip,
        referrer = referrer,
        browser = browser,
        platform = platform,
        country = country,
        os = os,
        city = city
    )
)

/**
 * Extension method to convert a domain [Click] into a [ClickEntity].
 */
fun Click.toEntity() = properties.browser.let {
    ClickEntity(
        id = null,
        hash = hash,
        created = created,
        ip = properties.ip,
        referrer = properties.referrer,
        os = properties.os,
        browser = it,
        platform = properties.platform,
        country = properties.country,
        city = properties.city
    )
}

/**
 * Extension method to convert a [ShortUrlEntity] into a domain [ShortUrl].
 */
fun ShortUrlEntity.toDomain() = ShortUrl(
    hash = hash,
    redirection = Redirection(
        target = target,
        mode = mode
    ),
    created = created,
    properties = ShortUrlProperties(
        sponsor = sponsor,
        owner = owner,
        safe = safe,
        ip = ip,
        country = country,
        limit = limit,
        hayQr = hayQr,
        alcanzable = alcanzable
    )
)

/**
 * Extension method to convert a domain [ShortUrl] into a [ShortUrlEntity].
 */
fun ShortUrl.toEntity() = ShortUrlEntity(
    hash = hash,
    target = redirection.target,
    mode = redirection.mode,
    created = created,
    owner = properties.owner,
    sponsor = properties.sponsor,
    safe = properties.safe,
    ip = properties.ip,
    country = properties.country,
    limit = properties.limit,
    hayQr = properties.hayQr,
    alcanzable = properties.alcanzable
)
