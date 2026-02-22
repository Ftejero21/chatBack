package com.chat.chat.Controller;

import com.chat.chat.DTO.InviteDecisionDTO;
import com.chat.chat.Service.GroupInviteService.GroupInviteService;
import com.chat.chat.Utils.Constantes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Constantes.API_GROUP_INVITES)
@CrossOrigin("*")
public class GroupInviteController {

    @Autowired
    private GroupInviteService groupInviteService;

    @PostMapping(Constantes.GROUP_INVITE_ACCEPT)
    public void accept(@PathVariable("inviteId") Long inviteId, @RequestBody InviteDecisionDTO body) {
        groupInviteService.accept(inviteId, body.getUserId());
    }

    @PostMapping(Constantes.GROUP_INVITE_DECLINE)
    public void decline(@PathVariable("inviteId") Long inviteId, @RequestBody InviteDecisionDTO body) {
        groupInviteService.decline(inviteId, body.getUserId());
    }
}
