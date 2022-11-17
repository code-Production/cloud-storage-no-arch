package com.geekbrains.netty.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode
@AllArgsConstructor
public class AbstractCommand implements Serializable  {

    private Commands command;

}
