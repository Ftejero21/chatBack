package com.chat.chat.Service.AiService;

public interface AiMessageSearchScopeResolverService {

    AiMessageSearchScopeDTO resolverScope(Long userId, String consulta, Boolean incluirGrupales, Boolean incluirIndividuales);

    AiMessageSearchScopeDTO resolverScope(Long userId, String consulta, Boolean incluirGrupales, Boolean incluirIndividuales, AiMessageSearchNaturalQueryAnalysis analysis);
}
